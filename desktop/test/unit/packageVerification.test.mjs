import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { verifyPackage } from '../../scripts/verify-package.mjs';

let root;
afterEach(async () => { if (root) await rm(root, { recursive: true, force: true }); });
async function fixture() {
  root = await mkdtemp(path.join(tmpdir(), 'desktop-package-'));
  for (const file of [
    'LyricVocabularyBuilder.exe', 'resources/app.asar', 'resources/backend/backend-1.0.0.jar',
    'resources/runtime/bin/java.exe', 'resources/web/index.html', 'resources/resources-manifest.json',
  ]) {
    await mkdir(path.dirname(path.join(root, file)), { recursive: true });
    await writeFile(path.join(root, file), 'fixture');
  }
  await writeFile(path.join(root, 'resources/runtime/release'), 'JAVA_VERSION="25.0.4"');
}
describe('package verification', () => {
  it('requires portable.flag only for portable artifacts', async () => {
    await fixture();
    await verifyPackage(root, false);
    await expect(verifyPackage(root, true)).rejects.toThrow('portable.flag');
    await writeFile(path.join(root, 'portable.flag'), '');
    await verifyPackage(root, true);
    await expect(verifyPackage(root, false)).rejects.toThrow('portable.flag');
  });
  it('rejects mutable data accidentally included in resources', async () => {
    await fixture();
    await writeFile(path.join(root, 'resources/web/app.db'), 'private');
    await expect(verifyPackage(root, false)).rejects.toThrow('mutable user data');
  });
  it('rejects packages with missing bundled Java', async () => {
    await fixture();
    await rm(path.join(root, 'resources/runtime/bin/java.exe'));
    await expect(verifyPackage(root, false)).rejects.toThrow();
  });
});
