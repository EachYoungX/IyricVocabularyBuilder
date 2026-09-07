import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises';
import { basename, dirname, isAbsolute, win32 } from 'node:path';

export const RUNTIME_CONFIG_SCHEMA_VERSION = 1;

export type RuntimeConfig = {
  schemaVersion: 1;
  dataset: {
    mode: 'managed' | 'external';
    fileName: string;
    externalPath: string | null;
  };
  lan: {
    enabled: boolean;
  };
};

export function defaultRuntimeConfig(): RuntimeConfig {
  return {
    schemaVersion: RUNTIME_CONFIG_SCHEMA_VERSION,
    dataset: {
      mode: 'managed',
      fileName: 'dictionary.sqlite',
      externalPath: null,
    },
    lan: { enabled: false },
  };
}

export class RuntimeConfigStore {
  constructor(private readonly configFile: string) {}

  async load(): Promise<RuntimeConfig> {
    let content: string;
    try {
      content = await readFile(this.configFile, 'utf8');
    } catch (error) {
      if (isNodeError(error) && error.code === 'ENOENT') {
        const initial = defaultRuntimeConfig();
        await this.save(initial);
        return initial;
      }
      throw configError(this.configFile, error);
    }

    let parsed: unknown;
    let normalized: RuntimeConfig;
    try {
      parsed = JSON.parse(content);
      normalized = migrateAndValidate(parsed);
    } catch (error) {
      throw configError(this.configFile, error);
    }
    if (isRecord(parsed) && parsed.schemaVersion !== RUNTIME_CONFIG_SCHEMA_VERSION) {
      await this.save(normalized);
    }
    return normalized;
  }

  async save(config: RuntimeConfig): Promise<RuntimeConfig> {
    const normalized = migrateAndValidate(config);
    const temporaryFile = `${this.configFile}.tmp`;
    await mkdir(dirname(this.configFile), { recursive: true });
    try {
      await writeFile(temporaryFile, `${JSON.stringify(normalized, null, 2)}\n`, {
        encoding: 'utf8',
        mode: 0o600,
      });
      await rename(temporaryFile, this.configFile);
      return normalized;
    } catch (error) {
      await rm(temporaryFile, { force: true }).catch(() => undefined);
      throw configError(this.configFile, error);
    }
  }
}

export function migrateAndValidate(value: unknown): RuntimeConfig {
  if (!isRecord(value)) throw new Error('configuration must be an object');
  const schemaVersion = value.schemaVersion ?? 0;
  if (schemaVersion !== 0 && schemaVersion !== RUNTIME_CONFIG_SCHEMA_VERSION) {
    throw new Error(`unsupported schemaVersion: ${String(schemaVersion)}`);
  }
  if (!isRecord(value.dataset)) throw new Error('dataset configuration is required');
  if (!isRecord(value.lan) || typeof value.lan.enabled !== 'boolean') {
    throw new Error('lan.enabled must be a boolean');
  }

  const mode = value.dataset.mode;
  if (mode !== 'managed' && mode !== 'external') throw new Error('dataset.mode is invalid');
  const fileName = value.dataset.fileName;
  if (typeof fileName !== 'string' || !validFileName(fileName)) {
    throw new Error('dataset.fileName must be a plain file name');
  }
  const externalPath = value.dataset.externalPath;
  if (mode === 'managed' && externalPath !== null && externalPath !== undefined) {
    throw new Error('managed dataset cannot define externalPath');
  }
  if (mode === 'external' && (typeof externalPath !== 'string' || !absoluteOnAnyPlatform(externalPath))) {
    throw new Error('external dataset requires an absolute externalPath');
  }

  return {
    schemaVersion: RUNTIME_CONFIG_SCHEMA_VERSION,
    dataset: {
      mode,
      fileName,
      externalPath: mode === 'external' ? externalPath as string : null,
    },
    lan: { enabled: value.lan.enabled },
  };
}

function validFileName(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 && trimmed === value && basename(value) === value && win32.basename(value) === value;
}

function absoluteOnAnyPlatform(value: string) {
  return isAbsolute(value) || win32.isAbsolute(value);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isNodeError(error: unknown): error is NodeJS.ErrnoException {
  return error instanceof Error && 'code' in error;
}

function configError(configFile: string, error: unknown) {
  const detail = error instanceof Error ? error.message : String(error);
  return new Error(`Invalid runtime configuration at ${configFile}: ${detail}`);
}
