import { ipcMain } from 'electron';
import type { DesktopMode } from '../paths/modeResolver';

export type DesktopRuntimeInfo = {
  appVersion: string;
  backendUrl: string;
  backendVersion: string;
  mode: DesktopMode;
};

export function registerIpc(runtimeInfo: DesktopRuntimeInfo) {
  ipcMain.removeHandler('desktop:get-runtime-info');
  ipcMain.handle('desktop:get-runtime-info', () => runtimeInfo);
}
