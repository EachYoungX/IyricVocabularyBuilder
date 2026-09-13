import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RuntimeConfigStore, defaultRuntimeConfig } from '../../src/config/runtimeConfig';
import { resetDesktopData } from '../../src/data/resetDesktopData';
import { prepareAppPaths, resolveAppPaths } from '../../src/paths/appPaths';

let root: string;
afterEach(async () => { if (root) await rm(root, { recursive: true, force: true }); });

async function fixture() {
  root = await mkdtemp(join(tmpdir(), 'desktop-reset-'));
  const paths = resolveAppPaths({ mode: 'development', repositoryRoot: root, executablePath: '' });
  prepareAppPaths(paths);
  const configStore = new RuntimeConfigStore(paths.runtimeConfigFile);
  const external = join(root, 'external.sqlite');
  await writeFile(external, 'external');
  await writeFile(join(root, 'backup.json'), 'backup');
  await writeFile(join(paths.datasetsDir, 'dictionary.sqlite'), 'managed');
  await writeFile(paths.databaseFile, 'transaction-owned database');
  await writeFile(join(paths.configDir, 'welcome-dismissed'), 'true');
  await writeFile(join(paths.cacheDir, 'cache'), 'cache');
  await mkdir(join(paths.tempDir, 'nested'));
  await writeFile(join(paths.tempDir, 'nested', 'partial'), 'partial');
  await configStore.save({
    schemaVersion: 1,
    dataset: { mode: 'external', fileName: 'external.sqlite', externalPath: external },
    lan: { enabled: true },
  });
  return { paths, configStore, external };
}

describe('desktop reset', () => {
  it('resets shell state after the business transaction and preserves datasets and backups', async () => {
    const fixtureData = await fixture();
    const events: string[] = [];
    await resetDesktopData({
      ...fixtureData,
      clearBusinessData: async () => { events.push('transaction'); },
      stopBackend: async () => { events.push('stop'); },
      clearBrowserStorage: async () => { events.push('storage'); },
      startBackend: async () => {
        events.push('start');
        expect(await fixtureData.configStore.load()).toEqual(defaultRuntimeConfig());
      },
    });
    expect(events).toEqual(['transaction', 'stop', 'storage', 'start']);
    await expect(readFile(join(fixtureData.paths.configDir, 'welcome-dismissed'))).rejects.toMatchObject({ code: 'ENOENT' });
    expect(await readFile(fixtureData.external, 'utf8')).toBe('external');
    expect(await readFile(join(root, 'backup.json'), 'utf8')).toBe('backup');
    expect(await readFile(join(fixtureData.paths.datasetsDir, 'dictionary.sqlite'), 'utf8')).toBe('managed');
    expect(await readFile(fixtureData.paths.databaseFile, 'utf8')).toBe('transaction-owned database');
    await expect(readFile(join(fixtureData.paths.cacheDir, 'cache'))).rejects.toMatchObject({ code: 'ENOENT' });
    await expect(readFile(join(fixtureData.paths.tempDir, 'nested', 'partial'))).rejects.toMatchObject({ code: 'ENOENT' });
  });

  it('keeps shell state when the business transaction fails', async () => {
    const fixtureData = await fixture();
    const stopBackend = vi.fn();
    const clearBrowserStorage = vi.fn();
    const startBackend = vi.fn();
    await expect(resetDesktopData({
      ...fixtureData, stopBackend, clearBrowserStorage, startBackend,
      clearBusinessData: async () => { throw new Error('transaction failed'); },
    })).rejects.toThrow('transaction failed');
    expect(stopBackend).not.toHaveBeenCalled();
    expect(clearBrowserStorage).not.toHaveBeenCalled();
    expect(startBackend).not.toHaveBeenCalled();
    expect((await fixtureData.configStore.load()).lan.enabled).toBe(true);
  });

  it('restarts with LAN disabled even if browser cleanup fails', async () => {
    const fixtureData = await fixture();
    const startBackend = vi.fn();
    await expect(resetDesktopData({
      ...fixtureData, startBackend,
      clearBusinessData: async () => {},
      stopBackend: async () => {},
      clearBrowserStorage: async () => { throw new Error('storage failed'); },
    })).rejects.toThrow('storage failed');
    expect(startBackend).toHaveBeenCalledOnce();
    expect((await fixtureData.configStore.load()).lan.enabled).toBe(false);
  });
});
