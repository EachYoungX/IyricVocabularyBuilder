import { contextBridge, ipcRenderer } from 'electron';
import type { DesktopRuntimeInfo } from './ipc/registerIpc';

contextBridge.exposeInMainWorld('desktopBridge', {
  getRuntimeInfo: (): Promise<DesktopRuntimeInfo> => ipcRenderer.invoke('desktop:get-runtime-info'),
});
