import { randomUUID } from 'node:crypto';
import { copyFile, mkdir, readdir, rename, rm, stat } from 'node:fs/promises';
import path from 'node:path';
import type { RuntimeConfig } from '../config/runtimeConfig';
import { RuntimeConfigStore, defaultRuntimeConfig } from '../config/runtimeConfig';
import type { AppPaths } from '../paths/appPaths';
import type { DictionaryProbe, DictionaryProbeResult } from './dictionaryProbe';

export type DatasetDescriptor = {
  source: 'managed' | 'external';
  path: string;
  fileName: string;
  datasetVersion?: string;
  schemaVersion?: string;
  size: number;
  lastModified: number;
  status: 'valid' | 'invalid' | 'missing' | 'incompatible';
  message?: string;
};

export type DatasetState = {
  active: DatasetDescriptor | null;
  managed: DatasetDescriptor[];
  dictionaryEnabled: boolean;
  autoSelected: boolean;
};

type RestartBackend = (dictionaryFile: string | null) => Promise<void>;

export class DatasetManager {
  private importInProgress = false;

  constructor(
    private readonly appPaths: AppPaths,
    private readonly configStore: RuntimeConfigStore,
    private readonly probe: Pick<DictionaryProbe, 'probe'>,
    private readonly restartBackend: RestartBackend,
  ) {}

  async initialize(): Promise<DatasetState> {
    const config = await this.configStore.load();
    const managed = await this.scanManaged();
    let active = await this.describeConfigured(config);
    let autoSelected = false;
    if (config.dataset.mode === 'managed' && active?.status !== 'valid') {
      const valid = managed.filter((candidate) => candidate.status === 'valid');
      if (valid.length === 1) {
        const selected = valid[0]!;
        await this.configStore.save(managedConfig(selected.fileName, config.lan));
        active = selected;
        autoSelected = true;
      }
    }
    return state(active, managed, autoSelected);
  }

  async getState(): Promise<DatasetState> {
    const [config, managed] = await Promise.all([
      this.configStore.load(),
      this.scanManaged(),
    ]);
    return state(await this.describeConfigured(config), managed, false);
  }

  async activeDictionaryFile() {
    const active = await this.describeConfigured(await this.configStore.load());
    return active?.status === 'valid' ? active.path : null;
  }

  async scanManaged(): Promise<DatasetDescriptor[]> {
    await mkdir(this.appPaths.datasetsDir, { recursive: true });
    const entries = await readdir(this.appPaths.datasetsDir, { withFileTypes: true });
    const candidates = entries
      .filter((entry) => entry.isFile())
      .sort((left, right) => left.name.localeCompare(right.name));
    const descriptors: DatasetDescriptor[] = [];
    for (const entry of candidates) {
      descriptors.push(await this.describe(
        'managed', path.join(this.appPaths.datasetsDir, entry.name),
      ));
    }
    return descriptors;
  }

  async importManaged(sourcePath: string): Promise<DatasetDescriptor> {
    if (this.importInProgress) throw new Error('A managed dataset import is already in progress');
    this.importInProgress = true;
    const incomingDirectory = path.join(this.appPaths.datasetsDir, '.incoming');
    let partialFile: string | null = null;
    try {
      if (!path.isAbsolute(sourcePath)) throw new Error('Dataset import source must be an absolute path');
      const sourceInfo = await stat(sourcePath);
      if (!sourceInfo.isFile()) throw new Error('Dataset import source must be a file');
      await mkdir(incomingDirectory, { recursive: true });
      const fileName = path.basename(sourcePath);
      const destination = managedPath(this.appPaths, fileName);
      if (await exists(destination)) throw new Error(`Managed dataset already exists: ${fileName}`);
      partialFile = path.join(incomingDirectory, `${randomUUID()}.partial`);
      await copyFile(sourcePath, partialFile);
      const probe = await this.probe.probe(partialFile);
      if (probe.status !== 'valid') {
        throw new Error(probe.message || `Dataset is ${probe.status}`);
      }
      await rename(partialFile, destination);
      partialFile = null;
      return this.describe('managed', destination, probe);
    } finally {
      if (partialFile) await rm(partialFile, { force: true }).catch(() => undefined);
      this.importInProgress = false;
    }
  }

  async activateManaged(fileName: string): Promise<DatasetState> {
    const candidate = await this.describe('managed', managedPath(this.appPaths, fileName));
    assertValid(candidate);
    const current = await this.configStore.load();
    await this.switchConfig(managedConfig(candidate.fileName, current.lan), candidate.path);
    return this.getState();
  }

  async activateExternal(candidatePath: string): Promise<DatasetState> {
    if (!path.isAbsolute(candidatePath)) throw new Error('External dataset path must be absolute');
    const candidate = await this.describe('external', candidatePath);
    assertValid(candidate);
    const config: RuntimeConfig = {
      schemaVersion: 1,
      dataset: {
        mode: 'external',
        fileName: candidate.fileName,
        externalPath: candidate.path,
      },
      lan: (await this.configStore.load()).lan,
    };
    await this.switchConfig(config, candidate.path);
    return this.getState();
  }

  async clearExternalReference(): Promise<DatasetState> {
    const current = await this.configStore.load();
    if (current.dataset.mode !== 'external') return this.getState();
    const next = defaultRuntimeConfig();
    next.lan = current.lan;
    const descriptor = await this.describeConfigured(next);
    await this.switchConfig(next, descriptor?.status === 'valid' ? descriptor.path : null);
    return this.getState();
  }

  async removeManaged(fileName: string): Promise<DatasetState> {
    const target = managedPath(this.appPaths, fileName);
    const current = await this.configStore.load();
    if (current.dataset.mode === 'managed' && current.dataset.fileName === fileName) {
      const next = defaultRuntimeConfig();
      next.lan = current.lan;
      await this.switchConfig(next, null);
    }
    await rm(target, { force: true });
    return this.getState();
  }

  private async switchConfig(next: RuntimeConfig, nextDictionaryFile: string | null) {
    const previous = await this.configStore.load();
    const previousDescriptor = await this.describeConfigured(previous);
    const previousDictionaryFile = previousDescriptor?.status === 'valid'
      ? previousDescriptor.path
      : null;
    await this.configStore.save(next);
    try {
      await this.restartBackend(nextDictionaryFile);
    } catch (switchError) {
      await this.configStore.save(previous);
      try {
        await this.restartBackend(previousDictionaryFile);
      } catch (rollbackError) {
        throw new AggregateError(
          [switchError, rollbackError],
          'Dataset switch failed and the previous backend could not be restored',
          { cause: rollbackError },
        );
      }
      throw new Error('Dataset switch failed; the previous dataset was restored', { cause: switchError });
    }
  }

  private async describeConfigured(config: RuntimeConfig) {
    if (config.dataset.mode === 'external') {
      return this.describe('external', config.dataset.externalPath as string);
    }
    return this.describe('managed', managedPath(this.appPaths, config.dataset.fileName));
  }

  private async describe(
    source: DatasetDescriptor['source'],
    candidatePath: string,
    knownProbe?: DictionaryProbeResult,
  ): Promise<DatasetDescriptor> {
    const candidate = path.resolve(candidatePath);
    let details;
    try {
      details = await stat(candidate);
    } catch (error) {
      if (isNodeError(error) && error.code === 'ENOENT') {
        return {
          source,
          path: candidate,
          fileName: path.basename(candidate),
          size: 0,
          lastModified: 0,
          status: 'missing',
          message: 'Dataset file is missing',
        };
      }
      throw error;
    }
    if (!details.isFile()) throw new Error(`Dataset path is not a file: ${candidate}`);
    const result = knownProbe ?? await this.probe.probe(candidate);
    return {
      source,
      path: candidate,
      fileName: path.basename(candidate),
      datasetVersion: result.datasetVersion,
      schemaVersion: result.schemaVersion,
      size: details.size,
      lastModified: details.mtimeMs,
      status: result.status,
      message: result.message,
    };
  }
}

function state(
  active: DatasetDescriptor | null,
  managed: DatasetDescriptor[],
  autoSelected: boolean,
): DatasetState {
  return {
    active,
    managed,
    dictionaryEnabled: active?.status === 'valid',
    autoSelected,
  };
}

function managedConfig(fileName: string, lan: RuntimeConfig['lan'] = { enabled: false }): RuntimeConfig {
  return {
    schemaVersion: 1,
    dataset: { mode: 'managed', fileName, externalPath: null },
    lan,
  };
}

function managedPath(appPaths: AppPaths, fileName: string) {
  if (path.basename(fileName) !== fileName || !fileName.trim()) {
    throw new Error('Managed dataset fileName must be a plain file name');
  }
  const candidate = path.resolve(appPaths.datasetsDir, fileName);
  if (path.dirname(candidate) !== path.resolve(appPaths.datasetsDir)) {
    throw new Error('Managed dataset path escapes the datasets directory');
  }
  return candidate;
}

function assertValid(descriptor: DatasetDescriptor) {
  if (descriptor.status !== 'valid') {
    throw new Error(descriptor.message || `Dataset is ${descriptor.status}`);
  }
}

async function exists(candidate: string) {
  return stat(candidate).then(() => true, (error: unknown) => {
    if (isNodeError(error) && error.code === 'ENOENT') return false;
    throw error;
  });
}

function isNodeError(error: unknown): error is NodeJS.ErrnoException {
  return error instanceof Error && 'code' in error;
}
