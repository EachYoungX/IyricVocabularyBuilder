import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { BackendSupervisor } from '../../src/backend/backendSupervisor';

let temporaryRoot: string | null = null;

afterEach(async () => {
  if (temporaryRoot) await rm(temporaryRoot, { recursive: true, force: true });
  temporaryRoot = null;
});

describe('BackendSupervisor', () => {
  it('reports a readable failure when Java cannot be started', async () => {
    temporaryRoot = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-supervisor-'));
    const backendJar = join(temporaryRoot, 'backend.jar');
    const webRoot = join(temporaryRoot, 'web');
    await mkdir(webRoot);
    await writeFile(backendJar, 'not used');
    const supervisor = new BackendSupervisor({
      javaExecutable: 'missing-java-executable-for-desktop-test',
      backendJar,
      webRoot,
      dataRoot: temporaryRoot,
      logsDir: join(temporaryRoot, 'logs'),
      startupTimeoutMs: 1_000,
    });

    await expect(supervisor.start()).rejects.toThrow('Backend process failed to start');
    expect(supervisor.getState()).toBe('FAILED');
  });
});
