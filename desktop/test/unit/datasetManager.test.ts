import { mkdtemp, mkdir, readdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RuntimeConfigStore, defaultRuntimeConfig } from '../../src/config/runtimeConfig';
import { DatasetManager } from '../../src/dataset/datasetManager';
import type { DictionaryProbeResult } from '../../src/dataset/dictionaryProbe';
import { prepareAppPaths, resolveAppPaths } from '../../src/paths/appPaths';

const temporaryRoots: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map((root) => rm(root, { recursive: true, force: true })));
});

describe('DatasetManager', () => {
  it('scans only direct managed files and reports validator results', async () => {
    const fixture = await createFixture((candidate) => Promise.resolve(
      candidate.endsWith('valid.data') ? validProbe() : { status: 'invalid', message: 'not SQLite' },
    ));
    await writeFile(join(fixture.paths.datasetsDir, 'valid.data'), 'valid');
    await writeFile(join(fixture.paths.datasetsDir, 'broken.data'), 'broken');
    await mkdir(join(fixture.paths.datasetsDir, '.incoming'));
    await writeFile(join(fixture.paths.datasetsDir, '.incoming', 'ignored.partial'), 'partial');

    const datasets = await fixture.manager.scanManaged();

    expect(datasets.map(({ fileName, status }) => ({ fileName, status }))).toEqual([
      { fileName: 'broken.data', status: 'invalid' },
      { fileName: 'valid.data', status: 'valid' },
    ]);
    expect(fixture.probe).toHaveBeenCalledTimes(2);
  });

  it('auto-selects exactly one valid managed dataset while preserving other config', async () => {
    const fixture = await createFixture(() => Promise.resolve(validProbe()));
    const config = defaultRuntimeConfig();
    config.lan.enabled = true;
    await fixture.store.save(config);
    await writeFile(join(fixture.paths.datasetsDir, 'only.sqlite'), 'valid');

    const result = await fixture.manager.initialize();

    expect(result.autoSelected).toBe(true);
    expect(result.active?.fileName).toBe('only.sqlite');
    expect((await fixture.store.load()).lan.enabled).toBe(true);
    expect(fixture.restart).not.toHaveBeenCalled();
  });

  it('does not randomly select when multiple valid managed datasets exist', async () => {
    const fixture = await createFixture(() => Promise.resolve(validProbe()));
    await writeFile(join(fixture.paths.datasetsDir, 'one.sqlite'), 'one');
    await writeFile(join(fixture.paths.datasetsDir, 'two.sqlite'), 'two');

    const result = await fixture.manager.initialize();

    expect(result.autoSelected).toBe(false);
    expect(result.dictionaryEnabled).toBe(false);
    expect(result.active?.status).toBe('missing');
  });

  it('imports through a hidden partial and removes it after failed validation', async () => {
    const fixture = await createFixture((candidate) => {
      expect(candidate).toContain(`${join(fixture.paths.datasetsDir, '.incoming')}`);
      expect(candidate).toMatch(/\.partial$/);
      return Promise.resolve({ status: 'invalid', message: 'broken schema' });
    });
    const source = join(fixture.root, 'source.sqlite');
    await writeFile(source, 'invalid');

    await expect(fixture.manager.importManaged(source)).rejects.toThrow('broken schema');

    expect(await readdir(join(fixture.paths.datasetsDir, '.incoming'))).toEqual([]);
    expect(await readdir(fixture.paths.datasetsDir)).toEqual(['.incoming']);
  });

  it('restores the previous config and backend when a dataset switch fails', async () => {
    const fixture = await createFixture(() => Promise.resolve(validProbe()));
    await writeFile(join(fixture.paths.datasetsDir, 'old.sqlite'), 'old');
    await writeFile(join(fixture.paths.datasetsDir, 'new.sqlite'), 'new');
    const oldConfig = defaultRuntimeConfig();
    oldConfig.dataset.fileName = 'old.sqlite';
    await fixture.store.save(oldConfig);
    fixture.restart.mockRejectedValueOnce(new Error('new backend failed')).mockResolvedValueOnce();

    await expect(fixture.manager.activateManaged('new.sqlite'))
      .rejects.toThrow('previous dataset was restored');

    expect((await fixture.store.load()).dataset.fileName).toBe('old.sqlite');
    expect(fixture.restart).toHaveBeenNthCalledWith(1, join(fixture.paths.datasetsDir, 'new.sqlite'));
    expect(fixture.restart).toHaveBeenNthCalledWith(2, join(fixture.paths.datasetsDir, 'old.sqlite'));
  });

  it('degrades a missing external dataset to no-dictionary mode without probing', async () => {
    const fixture = await createFixture(() => Promise.resolve(validProbe()));
    await fixture.store.save({
      schemaVersion: 1,
      dataset: {
        mode: 'external',
        fileName: 'missing.sqlite',
        externalPath: join(fixture.root, 'missing.sqlite'),
      },
      lan: { enabled: false },
    });

    const result = await fixture.manager.initialize();

    expect(result.active?.status).toBe('missing');
    expect(result.dictionaryEnabled).toBe(false);
    expect(fixture.probe).not.toHaveBeenCalled();
  });
});

async function createFixture(
  probeImplementation: (candidate: string) => Promise<DictionaryProbeResult>,
) {
  const root = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-dataset-'));
  temporaryRoots.push(root);
  const paths = resolveAppPaths({
    mode: 'development',
    executablePath: '/usr/bin/electron',
    repositoryRoot: root,
  });
  prepareAppPaths(paths);
  const store = new RuntimeConfigStore(paths.runtimeConfigFile);
  const probe = vi.fn(probeImplementation);
  const restart = vi.fn<(_: string | null) => Promise<void>>().mockResolvedValue();
  const manager = new DatasetManager(paths, store, { probe }, restart);
  return { root, paths, store, probe, restart, manager };
}

function validProbe(): DictionaryProbeResult {
  return { status: 'valid', schemaVersion: '1', datasetVersion: '2026.09' };
}
