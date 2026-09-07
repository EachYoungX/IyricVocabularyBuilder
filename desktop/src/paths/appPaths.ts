import {
  closeSync,
  fsyncSync,
  mkdirSync,
  openSync,
  unlinkSync,
  writeSync,
} from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import type { DesktopMode } from './modeResolver';

type PathApi = Pick<typeof path, 'dirname' | 'join' | 'resolve'>;

export type AppPaths = {
  mode: DesktopMode;
  root: string;
  dataDir: string;
  databaseFile: string;
  datasetsDir: string;
  configDir: string;
  runtimeConfigFile: string;
  cacheDir: string;
  logsDir: string;
  tempDir: string;
  electronUserDataDir: string;
  electronSessionDir: string;
};

export type AppPathOptions = {
  mode: DesktopMode;
  executablePath: string;
  localAppData?: string;
  repositoryRoot?: string;
  pathApi?: PathApi;
};

export function resolveAppPaths(options: AppPathOptions): AppPaths {
  const pathApi = options.pathApi ?? path;
  const root = resolveRoot(options, pathApi);
  const dataDir = pathApi.join(root, 'data');
  const configDir = pathApi.join(root, 'config');
  const electronRoot = pathApi.join(root, 'electron');
  return {
    mode: options.mode,
    root,
    dataDir,
    databaseFile: pathApi.join(dataDir, 'app.db'),
    datasetsDir: pathApi.join(root, 'datasets'),
    configDir,
    runtimeConfigFile: pathApi.join(configDir, 'runtime.json'),
    cacheDir: pathApi.join(root, 'cache'),
    logsDir: pathApi.join(root, 'logs'),
    tempDir: pathApi.join(root, 'temp'),
    electronUserDataDir: pathApi.join(electronRoot, 'user-data'),
    electronSessionDir: pathApi.join(electronRoot, 'session'),
  };
}

export function prepareAppPaths(appPaths: AppPaths) {
  try {
    mkdirSync(appPaths.root, { recursive: true });
    if (appPaths.mode === 'portable') verifyWritableRoot(appPaths.root);
    for (const directory of [
      appPaths.dataDir,
      appPaths.datasetsDir,
      appPaths.configDir,
      appPaths.cacheDir,
      appPaths.logsDir,
      appPaths.tempDir,
      appPaths.electronUserDataDir,
      appPaths.electronSessionDir,
    ]) {
      mkdirSync(directory, { recursive: true });
    }
  } catch (error) {
    const detail = error instanceof Error ? `: ${error.message}` : '';
    throw new Error(
      `Cannot initialize ${appPaths.mode} data directory at ${appPaths.root}${detail}`,
      { cause: error },
    );
  }
}

export type ElectronPathSetter = {
  setPath(name: 'userData' | 'sessionData', path: string): void;
  setAppLogsPath(path: string): void;
};

export function configureElectronDataPaths(electronApp: ElectronPathSetter, appPaths: AppPaths) {
  electronApp.setPath('userData', appPaths.electronUserDataDir);
  electronApp.setPath('sessionData', appPaths.electronSessionDir);
  electronApp.setAppLogsPath(appPaths.logsDir);
}

function resolveRoot(options: AppPathOptions, pathApi: PathApi) {
  if (options.mode === 'development') {
    if (!options.repositoryRoot) throw new Error('Development mode requires repositoryRoot');
    return pathApi.resolve(options.repositoryRoot, '.desktop-dev');
  }
  if (options.mode === 'portable') {
    return pathApi.resolve(pathApi.dirname(options.executablePath), 'user-data');
  }
  if (!options.localAppData?.trim()) throw new Error('Installed mode requires LOCALAPPDATA');
  return pathApi.resolve(options.localAppData, 'LyricVocabularyBuilder');
}

function verifyWritableRoot(root: string) {
  const probe = path.join(root, `.write-probe-${process.pid}-${randomUUID()}`);
  let descriptor: number | null = null;
  try {
    descriptor = openSync(probe, 'wx', 0o600);
    writeSync(descriptor, 'portable-write-probe');
    fsyncSync(descriptor);
    closeSync(descriptor);
    descriptor = null;
    unlinkSync(probe);
  } finally {
    if (descriptor !== null) closeSync(descriptor);
    try {
      unlinkSync(probe);
    } catch {
      // The successful path already removed the probe; failed cleanup is reported by the outer operation.
    }
  }
}
