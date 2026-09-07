import { access, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path, { join, win32 } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  configureElectronDataPaths,
  prepareAppPaths,
  resolveAppPaths,
} from '../../src/paths/appPaths';

const temporaryRoots: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map((root) => rm(root, { recursive: true, force: true })));
});

describe('resolveAppPaths', () => {
  it('uses the fixed LocalAppData root for installed mode', () => {
    const paths = resolveAppPaths({
      mode: 'installed',
      executablePath: String.raw`C:\Program Files\LyricVocabularyBuilder\LyricVocabularyBuilder.exe`,
      localAppData: String.raw`C:\Users\Test User\AppData\Local`,
      pathApi: win32,
    });

    expect(paths.root).toBe(String.raw`C:\Users\Test User\AppData\Local\LyricVocabularyBuilder`);
    expect(paths.databaseFile).toBe(String.raw`C:\Users\Test User\AppData\Local\LyricVocabularyBuilder\data\app.db`);
  });

  it('keeps portable data beside the executable and development data inside the repository', () => {
    const portable = resolveAppPaths({
      mode: 'portable',
      executablePath: String.raw`D:\软件\歌词词汇\LyricVocabularyBuilder.exe`,
      pathApi: win32,
    });
    const development = resolveAppPaths({
      mode: 'development',
      executablePath: '/usr/bin/electron',
      repositoryRoot: '/workspace/project',
      pathApi: path.posix,
    });

    expect(portable.root).toBe(String.raw`D:\软件\歌词词汇\user-data`);
    expect(portable.runtimeConfigFile).toBe(String.raw`D:\软件\歌词词汇\user-data\config\runtime.json`);
    expect(development.root).toBe('/workspace/project/.desktop-dev');
  });

  it('creates every data directory and redirects Electron-owned storage', async () => {
    const repositoryRoot = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-paths-'));
    temporaryRoots.push(repositoryRoot);
    const paths = resolveAppPaths({
      mode: 'development',
      executablePath: '/usr/bin/electron',
      repositoryRoot,
    });
    prepareAppPaths(paths);
    await Promise.all([
      access(paths.dataDir),
      access(paths.datasetsDir),
      access(paths.electronUserDataDir),
      access(paths.electronSessionDir),
    ]);
    const electronApp = { setPath: vi.fn(), setAppLogsPath: vi.fn() };

    configureElectronDataPaths(electronApp, paths);

    expect(electronApp.setPath).toHaveBeenCalledWith('userData', paths.electronUserDataDir);
    expect(electronApp.setPath).toHaveBeenCalledWith('sessionData', paths.electronSessionDir);
    expect(electronApp.setAppLogsPath).toHaveBeenCalledWith(paths.logsDir);
  });

  it('fails portable startup when its fixed user-data root cannot be created', async () => {
    const portableRoot = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-readonly-'));
    temporaryRoots.push(portableRoot);
    const executablePath = join(portableRoot, 'LyricVocabularyBuilder.exe');
    const paths = resolveAppPaths({ mode: 'portable', executablePath });
    await writeFile(paths.root, 'blocks directory creation');

    expect(() => prepareAppPaths(paths)).toThrow(`Cannot initialize portable data directory at ${paths.root}`);
  });
});
