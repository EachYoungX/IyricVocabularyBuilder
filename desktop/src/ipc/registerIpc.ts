import { ipcMain } from 'electron';

export type DesktopRuntimeInfo = {
  appVersion: string;
  backendUrl: string;
  backendVersion: string;
  shellMode: 'development' | 'packaged';
};

export function registerIpc(runtimeInfo: DesktopRuntimeInfo) {
  ipcMain.removeHandler('desktop:get-runtime-info');
  ipcMain.handle('desktop:get-runtime-info', () => runtimeInfo);
}
