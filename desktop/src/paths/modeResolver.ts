import { existsSync } from 'node:fs';
import path from 'node:path';

export type DesktopMode = 'development' | 'installed' | 'portable';

type PathApi = Pick<typeof path, 'dirname' | 'join'>;

export type ModeResolverOptions = {
  isPackaged: boolean;
  executablePath: string;
  fileExists?: (candidate: string) => boolean;
  pathApi?: PathApi;
};

export function resolveDesktopMode(options: ModeResolverOptions): DesktopMode {
  if (!options.isPackaged) return 'development';
  const fileExists = options.fileExists ?? existsSync;
  return fileExists(portableFlagPath(options.executablePath, options.pathApi)) ? 'portable' : 'installed';
}

export function portableFlagPath(executablePath: string, pathApi: PathApi = path) {
  return pathApi.join(pathApi.dirname(executablePath), 'portable.flag');
}
