import { chmod, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { DictionaryProbe, sqliteReadOnlyJdbcUrl } from '../../src/dataset/dictionaryProbe';

const temporaryRoots: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map((root) => rm(root, { recursive: true, force: true })));
});

describe('DictionaryProbe', () => {
  it('parses a valid machine-readable probe result', async () => {
    const fixture = await createProbeScript(`#!/usr/bin/env bash
printf '%s\\n' '{"status":"valid","schemaVersion":"1","datasetVersion":"2026.09"}'
`);
    const probe = new DictionaryProbe({
      javaExecutable: fixture.executable,
      backendJar: fixture.backendJar,
    });

    await expect(probe.probe(join(fixture.root, '词典 file.sqlite'))).resolves.toEqual({
      status: 'valid',
      schemaVersion: '1',
      datasetVersion: '2026.09',
      message: undefined,
    });
  });

  it('returns incompatible probe details from a non-zero process', async () => {
    const fixture = await createProbeScript(`#!/usr/bin/env bash
printf '%s\\n' '{"status":"incompatible","message":"unsupported schema"}' >&2
exit 3
`);
    const probe = new DictionaryProbe({
      javaExecutable: fixture.executable,
      backendJar: fixture.backendJar,
    });

    await expect(probe.probe(join(fixture.root, 'old.sqlite'))).resolves.toEqual({
      status: 'incompatible',
      schemaVersion: undefined,
      datasetVersion: undefined,
      message: 'unsupported schema',
    });
  });

  it('encodes spaces and Unicode in the read-only SQLite URI', () => {
    expect(sqliteReadOnlyJdbcUrl('/tmp/词典 file.sqlite'))
      .toBe('jdbc:sqlite:file:///tmp/%E8%AF%8D%E5%85%B8%20file.sqlite?mode=ro');
  });

  it('terminates and rejects a probe that exceeds its timeout', async () => {
    const fixture = await createProbeScript(`#!/usr/bin/env bash
sleep 5
`);
    const probe = new DictionaryProbe({
      javaExecutable: fixture.executable,
      backendJar: fixture.backendJar,
      timeoutMs: 20,
    });

    await expect(probe.probe(join(fixture.root, 'slow.sqlite')))
      .rejects.toThrow('Dictionary probe timed out');
  });
});

async function createProbeScript(content: string) {
  const root = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-probe-'));
  temporaryRoots.push(root);
  const executable = join(root, 'fake-java');
  const backendJar = join(root, 'backend.jar');
  await writeFile(executable, content);
  await chmod(executable, 0o755);
  await writeFile(backendJar, 'not used by fake Java');
  return { root, executable, backendJar };
}
