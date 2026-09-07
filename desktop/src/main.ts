import { app, BrowserWindow, dialog } from 'electron';
import { join, resolve } from 'node:path';
import { BackendSupervisor } from './backend/backendSupervisor';
import { registerIpc } from './ipc/registerIpc';

let mainWindow: BrowserWindow | null = null;
let backendSupervisor: BackendSupervisor | null = null;
let shutdownStarted = false;

const singleInstanceLock = app.requestSingleInstanceLock();
if (!singleInstanceLock) {
  app.quit();
} else {
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
  void app.whenReady().then(startApplication);
}

async function startApplication() {
  try {
    const resources = resolveRuntimeResources();
    backendSupervisor = new BackendSupervisor({
      javaExecutable: resources.javaExecutable,
      backendJar: resources.backendJar,
      webRoot: resources.webRoot,
      dataRoot: resources.dataRoot,
      logsDir: join(resources.dataRoot, 'logs'),
      onUnexpectedExit: (error) => {
        dialog.showErrorBox('Lyric Vocabulary Builder backend stopped', error.message);
        app.quit();
      },
    });
    const backend = await backendSupervisor.start();
    registerIpc({
      appVersion: app.getVersion(),
      backendUrl: backend.baseUrl,
      backendVersion: backend.health.version,
      shellMode: app.isPackaged ? 'packaged' : 'development',
    });
    await createMainWindow(backend.baseUrl);
  } catch (error) {
    dialog.showErrorBox(
      'Lyric Vocabulary Builder failed to start',
      error instanceof Error ? error.message : String(error),
    );
    shutdownStarted = true;
    await backendSupervisor?.stop();
    app.exit(1);
  }
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
  const allowedOrigin = new URL(baseUrl).origin;
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', (event, targetUrl) => {
    if (new URL(targetUrl).origin !== allowedOrigin) event.preventDefault();
  });
  window.once('ready-to-show', () => window.show());
  window.once('closed', () => {
    if (mainWindow === window) mainWindow = null;
  });
  await window.loadURL(baseUrl);
}

function resolveRuntimeResources() {
  if (app.isPackaged) {
    return {
      javaExecutable: join(process.resourcesPath, 'runtime', 'bin', 'java.exe'),
      backendJar: join(process.resourcesPath, 'backend', 'backend-1.0.0.jar'),
      webRoot: join(process.resourcesPath, 'web'),
      dataRoot: app.getPath('userData'),
    };
  }

  const desktopRoot = resolve(__dirname, '..');
  const repositoryRoot = resolve(desktopRoot, '..');
  return {
    javaExecutable: process.env.DESKTOP_JAVA_EXECUTABLE || 'java',
    backendJar: process.env.DESKTOP_BACKEND_JAR
      ? resolve(process.env.DESKTOP_BACKEND_JAR)
      : join(repositoryRoot, 'backend', 'target', 'backend-1.0.0.jar'),
    webRoot: process.env.DESKTOP_WEB_ROOT
      ? resolve(process.env.DESKTOP_WEB_ROOT)
      : join(repositoryRoot, 'frontend', 'dist', 'spa'),
    dataRoot: join(repositoryRoot, '.desktop-dev'),
  };
}
