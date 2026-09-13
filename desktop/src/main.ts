import { app, BrowserWindow, dialog, session, shell } from 'electron';
import { join, resolve } from 'node:path';
import { BackendSupervisor } from './backend/backendSupervisor';
import { resetDesktopData } from './data/resetDesktopData';
import { RuntimeConfigStore } from './config/runtimeConfig';
import { DatasetManager, type DatasetState } from './dataset/datasetManager';
import { DictionaryProbe } from './dataset/dictionaryProbe';
import { registerIpc, type DesktopRuntimeInfo } from './ipc/registerIpc';
import { lanUrlsForPort } from './lan/lanAddresses';
import { LanManager } from './lan/lanManager';
import {
  configureElectronDataPaths,
  prepareAppPaths,
  resolveAppPaths,
  type AppPaths,
} from './paths/appPaths';
import { resolveDesktopMode } from './paths/modeResolver';

let mainWindow: BrowserWindow | null = null;
let backendSupervisor: BackendSupervisor | null = null;
let currentRuntimeInfo: DesktopRuntimeInfo | null = null;
let currentDatasetState: DatasetState | null = null;
let allowedBackendOrigin: string | null = null;
let shutdownStarted = false;

void initializeAndLaunch().catch(failStartup);

async function initializeAndLaunch() {
  const repositoryRoot = resolveRepositoryRoot();
  const mode = resolveDesktopMode({
    isPackaged: app.isPackaged,
    executablePath: process.execPath,
  });
  const appPaths = resolveAppPaths({
    mode,
    executablePath: process.execPath,
    localAppData: process.env.LOCALAPPDATA,
    repositoryRoot,
  });
  prepareAppPaths(appPaths);
  configureElectronDataPaths(app, appPaths);

  if (!app.requestSingleInstanceLock()) {
    app.quit();
    return;
  }
  registerApplicationEvents();
  const resources = resolveRuntimeResources(repositoryRoot);
  const configStore = new RuntimeConfigStore(appPaths.runtimeConfigFile);
  const datasetManager = new DatasetManager(
    appPaths,
    configStore,
    new DictionaryProbe({
      javaExecutable: resources.javaExecutable,
      backendJar: resources.backendJar,
      tempDir: appPaths.tempDir,
    }),
    async (dictionaryFile) => restartBackend(
      appPaths,
      resources,
      dictionaryFile,
      (await configStore.load()).lan.enabled,
    ),
  );
  currentDatasetState = await datasetManager.initialize();
  const lanManager = new LanManager(
    configStore,
    () => datasetManager.activeDictionaryFile(),
    (dictionaryFile, enabled) => restartBackend(appPaths, resources, dictionaryFile, enabled),
  );
  await app.whenReady();
  const config = await configStore.load();
  const backend = await startBackend(
    appPaths,
    resources,
    currentDatasetState.dictionaryEnabled ? currentDatasetState.active?.path ?? null : null,
    config.lan.enabled,
  );
  registerDesktopIpc(appPaths, datasetManager, lanManager, configStore);
  await createMainWindow(backend.baseUrl);
}

function registerApplicationEvents() {
  app.on('second-instance', () => {
    if (!mainWindow) return;
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.focus();
  });

  app.on('before-quit', (event) => {
    if (shutdownStarted || !backendSupervisor) return;
    event.preventDefault();
    shutdownStarted = true;
    void backendSupervisor.stop().finally(() => app.exit(0));
  });

  app.on('window-all-closed', () => app.quit());
  process.once('SIGINT', () => app.quit());
  process.once('SIGTERM', () => app.quit());
}

type RuntimeResources = ReturnType<typeof resolveRuntimeResources>;

async function startBackend(
  appPaths: AppPaths,
  resources: RuntimeResources,
  dictionaryFile: string | null,
  lanEnabled: boolean,
) {
  const supervisor = new BackendSupervisor({
    javaExecutable: resources.javaExecutable,
    backendJar: resources.backendJar,
    webRoot: resources.webRoot,
    dataRoot: appPaths.root,
    databaseFile: appPaths.databaseFile,
    dictionaryFile,
    logsDir: appPaths.logsDir,
    host: lanEnabled ? '0.0.0.0' : '127.0.0.1',
    onUnexpectedExit: (error) => {
      dialog.showErrorBox('Lyric Vocabulary Builder backend stopped', error.message);
      app.quit();
    },
  });
  backendSupervisor = supervisor;
  const backend = await supervisor.start();
  currentRuntimeInfo = {
    appVersion: app.getVersion(),
    backendUrl: backend.baseUrl,
    backendVersion: backend.health.version,
    mode: appPaths.mode,
    lanEnabled,
    lanUrls: lanEnabled ? lanUrlsForPort(backend.port) : [],
  };
  allowedBackendOrigin = new URL(backend.baseUrl).origin;
  return backend;
}

async function restartBackend(
  appPaths: AppPaths,
  resources: RuntimeResources,
  dictionaryFile: string | null,
  lanEnabled: boolean,
) {
  await backendSupervisor?.stop();
  await startBackend(appPaths, resources, dictionaryFile, lanEnabled);
}

async function createMainWindow(baseUrl: string) {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 960,
    minHeight: 640,
    show: false,
    webPreferences: {
      preload: join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
    },
  });
  mainWindow = window;
  window.setMenu(null);
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', (event, targetUrl) => {
    if (new URL(targetUrl).origin !== allowedBackendOrigin) event.preventDefault();
  });
  window.once('ready-to-show', () => window.show());
  window.once('closed', () => {
    if (mainWindow === window) mainWindow = null;
  });
  await window.loadURL(baseUrl);
}

function registerDesktopIpc(
  appPaths: AppPaths,
  datasetManager: DatasetManager,
  lanManager: LanManager,
  configStore: RuntimeConfigStore,
) {
  let mutationQueue: Promise<unknown> = Promise.resolve();
  const mutate = <T>(operation: () => Promise<T>) => {
    const result = mutationQueue.then(operation, operation);
    mutationQueue = result.then(() => undefined, () => undefined);
    return result;
  };
  const remember = (datasetState: DatasetState) => {
    currentDatasetState = datasetState;
    return datasetState;
  };
  const rememberAndReload = (datasetState: DatasetState) => {
    remember(datasetState);
    scheduleRendererReload();
    return datasetState;
  };

  registerIpc({
    getRuntimeInfo: () => {
      if (!currentRuntimeInfo) throw new Error('Desktop runtime is not ready');
      return currentRuntimeInfo;
    },
    getDatasetState: async () => {
      if (!currentDatasetState) currentDatasetState = await datasetManager.getState();
      const result = currentDatasetState;
      currentDatasetState = { ...result, autoSelected: false };
      return result;
    },
    openDataDirectory: () => openDirectory(appPaths.root),
    openDatasetDirectory: () => openDirectory(appPaths.datasetsDir),
    rescanDatasets: () => mutate(async () => remember(await datasetManager.getState())),
    importDatasetToManagedDirectory: () => mutate(async () => {
      const selected = await selectDatasetFile('Import dictionary into the managed directory');
      if (!selected) return null;
      const imported = await datasetManager.importManaged(selected);
      return rememberAndReload(await datasetManager.activateManaged(imported.fileName));
    }),
    selectExternalDataset: () => mutate(async () => {
      const selected = await selectDatasetFile('Select an external dictionary');
      if (!selected) return null;
      return rememberAndReload(await datasetManager.activateExternal(selected));
    }),
    activateManagedDataset: (fileName) => mutate(async () => (
      rememberAndReload(await datasetManager.activateManaged(requireFileName(fileName)))
    )),
    clearExternalDataset: () => mutate(async () => (
      rememberAndReload(await datasetManager.clearExternalReference())
    )),
    removeManagedDataset: (fileName) => mutate(async () => {
      const safeFileName = requireFileName(fileName);
      if (!await confirmManagedRemoval(safeFileName)) return datasetManager.getState();
      return rememberAndReload(await datasetManager.removeManaged(safeFileName));
    }),
    restartBackend: () => mutate(async () => {
      const dictionaryFile = await datasetManager.activeDictionaryFile();
      const resources = resolveRuntimeResources(resolveRepositoryRoot());
      const config = await configStore.load();
      await restartBackend(appPaths, resources, dictionaryFile, config.lan.enabled);
      if (!currentRuntimeInfo) throw new Error('Desktop runtime failed to restart');
      scheduleRendererReload();
      return currentRuntimeInfo;
    }),
    clearAllLocalData: () => mutate(async () => {
      try {
        await resetDesktopData({
          paths: appPaths,
          configStore,
          clearBusinessData: async () => {
            if (!currentRuntimeInfo) throw new Error('Desktop runtime is not ready');
            const response = await fetch(new URL('/api/data/all', currentRuntimeInfo.backendUrl), {
              method: 'DELETE',
              signal: AbortSignal.timeout(60_000),
            });
            if (!response.ok) throw new Error('Failed to clear application data');
          },
          stopBackend: async () => { await backendSupervisor?.stop(); },
          clearBrowserStorage: async () => {
            await session.defaultSession.clearStorageData();
            await session.defaultSession.clearCache();
          },
          startBackend: async () => {
            currentDatasetState = await datasetManager.initialize();
            const config = await configStore.load();
            await startBackend(
              appPaths, resolveRuntimeResources(resolveRepositoryRoot()),
              currentDatasetState.dictionaryEnabled ? currentDatasetState.active?.path ?? null : null,
              config.lan.enabled,
            );
          },
        });
      } finally {
        scheduleRendererReload();
      }
    }),
    setLanEnabled: (enabled) => mutate(async () => {
      if (typeof enabled !== 'boolean') throw new Error('LAN enabled value must be a boolean');
      await lanManager.setEnabled(enabled);
      if (!currentRuntimeInfo) throw new Error('Desktop runtime failed to restart');
      scheduleRendererReload();
      return currentRuntimeInfo;
    }),
  });
}

function scheduleRendererReload() {
  const backendUrl = currentRuntimeInfo?.backendUrl;
  if (!backendUrl) return;
  const timer = setTimeout(() => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    void mainWindow.loadURL(backendUrl).catch((error: unknown) => {
      dialog.showErrorBox(
        'Lyric Vocabulary Builder failed to reload',
        error instanceof Error ? error.message : String(error),
      );
    });
  }, 0);
  timer.unref();
}

async function openDirectory(directory: string) {
  const error = await shell.openPath(directory);
  if (error) throw new Error(`Cannot open directory: ${error}`);
}

async function selectDatasetFile(title: string) {
  const options: Electron.OpenDialogOptions = {
    title,
    properties: ['openFile'],
    filters: [
      { name: 'SQLite dictionaries', extensions: ['sqlite', 'sqlite3', 'db'] },
      { name: 'All files', extensions: ['*'] },
    ],
  };
  const selection = mainWindow
    ? await dialog.showOpenDialog(mainWindow, options)
    : await dialog.showOpenDialog(options);
  return selection.canceled ? null : selection.filePaths[0] ?? null;
}

async function confirmManagedRemoval(fileName: string) {
  const options: Electron.MessageBoxOptions = {
    type: 'warning',
    title: 'Remove managed dictionary',
    message: `Remove the application-managed copy “${fileName}”?`,
    detail: 'This deletes only the copy in the managed dictionary directory.',
    buttons: ['Cancel', 'Remove'],
    defaultId: 0,
    cancelId: 0,
    noLink: true,
  };
  const result = mainWindow
    ? await dialog.showMessageBox(mainWindow, options)
    : await dialog.showMessageBox(options);
  return result.response === 1;
}

function requireFileName(value: unknown) {
  if (typeof value !== 'string' || !value.trim()) throw new Error('A dataset fileName is required');
  return value;
}

function resolveRuntimeResources(repositoryRoot: string) {
  if (app.isPackaged) {
    return {
      javaExecutable: join(process.resourcesPath, 'runtime', 'bin', 'java.exe'),
      backendJar: join(process.resourcesPath, 'backend', 'backend-1.0.0.jar'),
      webRoot: join(process.resourcesPath, 'web'),
    };
  }

  return {
    javaExecutable: process.env.DESKTOP_JAVA_EXECUTABLE || 'java',
    backendJar: process.env.DESKTOP_BACKEND_JAR
      ? resolve(process.env.DESKTOP_BACKEND_JAR)
      : join(repositoryRoot, 'backend', 'target', 'backend-1.0.0.jar'),
    webRoot: process.env.DESKTOP_WEB_ROOT
      ? resolve(process.env.DESKTOP_WEB_ROOT)
      : join(repositoryRoot, 'frontend', 'dist', 'spa'),
  };
}

function resolveRepositoryRoot() {
  return resolve(__dirname, '..', '..');
}

async function failStartup(error: unknown) {
  dialog.showErrorBox(
    'Lyric Vocabulary Builder failed to start',
    error instanceof Error ? error.message : String(error),
  );
  shutdownStarted = true;
  await backendSupervisor?.stop();
  app.exit(1);
}
