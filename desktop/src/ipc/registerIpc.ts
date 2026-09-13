import { ipcMain } from 'electron';
import type { DatasetState } from '../dataset/datasetManager';
import type { DesktopMode } from '../paths/modeResolver';

export type DesktopRuntimeInfo = {
  appVersion: string;
  backendUrl: string;
  backendVersion: string;
  mode: DesktopMode;
  lanEnabled: boolean;
  lanUrls: string[];
};

export type DesktopIpcHandlers = {
  getWelcomeDismissed(): Promise<boolean>;
  dismissWelcome(): Promise<void>;
  openProjectPage(page: 'project' | 'dictionary'): Promise<void>;
  getRuntimeInfo(): DesktopRuntimeInfo;
  clearAllLocalData(): Promise<void>;
  getDatasetState(): Promise<DatasetState>;
  openDataDirectory(): Promise<void>;
  openDatasetDirectory(): Promise<void>;
  rescanDatasets(): Promise<DatasetState>;
  importDatasetToManagedDirectory(): Promise<DatasetState | null>;
  selectExternalDataset(): Promise<DatasetState | null>;
  activateManagedDataset(fileName: string): Promise<DatasetState>;
  clearExternalDataset(): Promise<DatasetState>;
  removeManagedDataset(fileName: string): Promise<DatasetState>;
  restartBackend(): Promise<DesktopRuntimeInfo>;
  setLanEnabled(enabled: boolean): Promise<DesktopRuntimeInfo>;
};

const channels = [
  'desktop:get-welcome-dismissed',
  'desktop:dismiss-welcome',
  'desktop:open-project-page',
  'desktop:clear-all-local-data',
  'desktop:get-runtime-info',
  'desktop:get-dataset-state',
  'desktop:open-data-directory',
  'desktop:open-dataset-directory',
  'desktop:rescan-datasets',
  'desktop:import-managed-dataset',
  'desktop:select-external-dataset',
  'desktop:activate-managed-dataset',
  'desktop:clear-external-dataset',
  'desktop:remove-managed-dataset',
  'desktop:restart-backend',
  'desktop:set-lan-enabled',
] as const;

export function registerIpc(handlers: DesktopIpcHandlers) {
  for (const channel of channels) ipcMain.removeHandler(channel);
  ipcMain.handle('desktop:get-welcome-dismissed', () => handlers.getWelcomeDismissed());
  ipcMain.handle('desktop:dismiss-welcome', () => handlers.dismissWelcome());
  ipcMain.handle('desktop:open-project-page', (_event, page: 'project' | 'dictionary') => handlers.openProjectPage(page));
  ipcMain.handle('desktop:clear-all-local-data', () => handlers.clearAllLocalData());
  ipcMain.handle('desktop:get-runtime-info', () => handlers.getRuntimeInfo());
  ipcMain.handle('desktop:get-dataset-state', () => handlers.getDatasetState());
  ipcMain.handle('desktop:open-data-directory', () => handlers.openDataDirectory());
  ipcMain.handle('desktop:open-dataset-directory', () => handlers.openDatasetDirectory());
  ipcMain.handle('desktop:rescan-datasets', () => handlers.rescanDatasets());
  ipcMain.handle('desktop:import-managed-dataset', () => handlers.importDatasetToManagedDirectory());
  ipcMain.handle('desktop:select-external-dataset', () => handlers.selectExternalDataset());
  ipcMain.handle('desktop:activate-managed-dataset', (_event, fileName: string) => (
    handlers.activateManagedDataset(fileName)
  ));
  ipcMain.handle('desktop:clear-external-dataset', () => handlers.clearExternalDataset());
  ipcMain.handle('desktop:remove-managed-dataset', (_event, fileName: string) => (
    handlers.removeManagedDataset(fileName)
  ));
  ipcMain.handle('desktop:restart-backend', () => handlers.restartBackend());
  ipcMain.handle('desktop:set-lan-enabled', (_event, enabled: boolean) => (
    handlers.setLanEnabled(enabled)
  ));
}
