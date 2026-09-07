import { access, mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import {
  defaultRuntimeConfig,
  migrateAndValidate,
  RuntimeConfigStore,
} from '../../src/config/runtimeConfig';

const temporaryRoots: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map((root) => rm(root, { recursive: true, force: true })));
});

async function createStore() {
  const root = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-config-'));
  temporaryRoots.push(root);
  const configFile = join(root, 'config', 'runtime.json');
  return { configFile, store: new RuntimeConfigStore(configFile) };
}

describe('RuntimeConfigStore', () => {
  it('creates the schema-versioned default config on first launch', async () => {
    const { configFile, store } = await createStore();

    await expect(store.load()).resolves.toEqual(defaultRuntimeConfig());
    expect(JSON.parse(await readFile(configFile, 'utf8'))).toEqual(defaultRuntimeConfig());
    await expect(access(`${configFile}.tmp`)).rejects.toMatchObject({ code: 'ENOENT' });
  });

  it('migrates an unversioned config, strips unknown fields, and persists schema version 1', async () => {
    const { configFile, store } = await createStore();
    await mkdir(dirname(configFile), { recursive: true });
    await writeFile(configFile, JSON.stringify({
      dataset: { mode: 'managed', fileName: 'custom.sqlite', externalPath: null, ignored: true },
      lan: { enabled: false },
      ignored: true,
    }));
    const expected = {
      schemaVersion: 1,
      dataset: { mode: 'managed', fileName: 'custom.sqlite', externalPath: null },
      lan: { enabled: false },
    };

    await expect(store.load()).resolves.toEqual(expected);
    expect(JSON.parse(await readFile(configFile, 'utf8'))).toEqual(expected);
    await expect(access(`${configFile}.tmp`)).rejects.toMatchObject({ code: 'ENOENT' });
  });

  it('atomically persists an external dataset using an absolute path', async () => {
    const { configFile, store } = await createStore();
    const config = {
      schemaVersion: 1 as const,
      dataset: {
        mode: 'external' as const,
        fileName: 'dictionary.sqlite',
        externalPath: String.raw`D:\Data\ECDICT\dictionary.sqlite`,
      },
      lan: { enabled: false },
    };

    await store.save(config);

    await expect(store.load()).resolves.toEqual(config);
    await expect(access(`${configFile}.tmp`)).rejects.toMatchObject({ code: 'ENOENT' });
  });

  it('rejects malformed and unsafe dataset paths', async () => {
    const { configFile, store } = await createStore();
    await mkdir(dirname(configFile), { recursive: true });
    await writeFile(configFile, JSON.stringify({
      schemaVersion: 1,
      dataset: { mode: 'managed', fileName: '../dictionary.sqlite', externalPath: null },
      lan: { enabled: false },
    }));

    await expect(store.load()).rejects.toThrow('dataset.fileName must be a plain file name');
    expect(() => migrateAndValidate({
      schemaVersion: 1,
      dataset: { mode: 'external', fileName: 'dictionary.sqlite', externalPath: 'relative.sqlite' },
      lan: { enabled: false },
    })).toThrow('external dataset requires an absolute externalPath');
  });
});
