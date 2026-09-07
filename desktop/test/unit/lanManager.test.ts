import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RuntimeConfigStore } from '../../src/config/runtimeConfig';
import { LanManager } from '../../src/lan/lanManager';

const temporaryRoots: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map((root) => rm(root, { recursive: true, force: true })));
});

describe('LanManager', () => {
  it('persists explicit enablement and restarts with the active dictionary', async () => {
    const fixture = await createFixture();

    await fixture.manager.setEnabled(true);

    expect((await fixture.store.load()).lan.enabled).toBe(true);
    expect(fixture.restart).toHaveBeenCalledWith('/datasets/current.sqlite', true);
  });

  it('restores the previous setting and backend when enabling fails', async () => {
    const fixture = await createFixture();
    fixture.restart.mockRejectedValueOnce(new Error('LAN bind failed')).mockResolvedValueOnce();

    await expect(fixture.manager.setEnabled(true)).rejects.toThrow('previous setting was restored');

    expect((await fixture.store.load()).lan.enabled).toBe(false);
    expect(fixture.restart).toHaveBeenNthCalledWith(1, '/datasets/current.sqlite', true);
    expect(fixture.restart).toHaveBeenNthCalledWith(2, '/datasets/current.sqlite', false);
  });
});

async function createFixture() {
  const root = await mkdtemp(join(tmpdir(), 'lyric-vocabulary-lan-'));
  temporaryRoots.push(root);
  const store = new RuntimeConfigStore(join(root, 'runtime.json'));
  await store.load();
  const restart = vi.fn<(_: string | null, __: boolean) => Promise<void>>().mockResolvedValue();
  const manager = new LanManager(
    store,
    () => Promise.resolve('/datasets/current.sqlite'),
    restart,
  );
  return { store, restart, manager };
}
