import { contextBridge, ipcRenderer } from 'electron';
import type { DatasetState } from './dataset/datasetManager';
import type { DesktopRuntimeInfo } from './ipc/registerIpc';

contextBridge.exposeInMainWorld('desktopBridge', {
  getRuntimeInfo: (): Promise<DesktopRuntimeInfo> => ipcRenderer.invoke('desktop:get-runtime-info'),
  getDatasetState: (): Promise<DatasetState> => ipcRenderer.invoke('desktop:get-dataset-state'),
  openDataDirectory: (): Promise<void> => ipcRenderer.invoke('desktop:open-data-directory'),
  openDatasetDirectory: (): Promise<void> => ipcRenderer.invoke('desktop:open-dataset-directory'),
  rescanDatasets: (): Promise<DatasetState> => ipcRenderer.invoke('desktop:rescan-datasets'),
  importDatasetToManagedDirectory: (): Promise<DatasetState | null> => (
    ipcRenderer.invoke('desktop:import-managed-dataset')
  ),
  selectExternalDataset: (): Promise<DatasetState | null> => (
    ipcRenderer.invoke('desktop:select-external-dataset')
  ),
  activateManagedDataset: (fileName: string): Promise<DatasetState> => (
    ipcRenderer.invoke('desktop:activate-managed-dataset', fileName)
  ),
  clearExternalDataset: (): Promise<DatasetState> => (
    ipcRenderer.invoke('desktop:clear-external-dataset')
  ),
  removeManagedDataset: (fileName: string): Promise<DatasetState> => (
    ipcRenderer.invoke('desktop:remove-managed-dataset', fileName)
  ),
  restartBackend: (): Promise<DesktopRuntimeInfo> => ipcRenderer.invoke('desktop:restart-backend'),
  setLanEnabled: (enabled: boolean): Promise<DesktopRuntimeInfo> => (
    ipcRenderer.invoke('desktop:set-lan-enabled', enabled)
  ),
});
