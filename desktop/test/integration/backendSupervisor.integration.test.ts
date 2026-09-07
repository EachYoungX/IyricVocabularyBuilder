import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { BackendSupervisor } from '../../src/backend/backendSupervisor';

let temporaryRoot: string | null = null;
let supervisor: BackendSupervisor | null = null;

afterEach(async () => {
  await supervisor?.stop();
  supervisor = null;
  if (temporaryRoot) await rm(temporaryRoot, { recursive: true, force: true });
  temporaryRoot = null;
});

describe('BackendSupervisor integration', () => {
  it('starts the packaged backend, serves the SPA, and exits gracefully', async () => {
    const repositoryRoot = resolve(process.cwd(), '..');
    temporaryRoot = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-desktop-'));
    supervisor = new BackendSupervisor({
      javaExecutable: process.env.DESKTOP_JAVA_EXECUTABLE || 'java',
      backendJar: join(repositoryRoot, 'backend', 'target', 'backend-1.0.0.jar'),
      webRoot: join(repositoryRoot, 'frontend', 'dist', 'spa'),
      dataRoot: temporaryRoot,
      logsDir: join(temporaryRoot, 'logs'),
      startupTimeoutMs: 40_000,
    });

    const runtime = await supervisor.start();
    expect(runtime.health).toEqual({ status: 'UP', version: '1.0.0' });
    expect(supervisor.getState()).toBe('READY');
    const page = await fetch(runtime.baseUrl);
    expect(page.ok).toBe(true);
    expect(await page.text()).toContain('<title>Lyric Vocabulary Builder</title>');

    await supervisor.stop();
    expect(supervisor.getState()).toBe('STOPPED');
    expect(await readFile(join(temporaryRoot, 'logs', 'backend.log'), 'utf8'))
      .toContain('Shutdown completed');
  }, 60_000);
});
