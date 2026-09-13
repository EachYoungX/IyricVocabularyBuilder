import { lstat, readdir, readFile } from 'node:fs/promises';
import path from 'node:path';

export async function verifyPackage(root, portable) {
  for (const file of [
    'LyricVocabularyBuilder.exe', 'resources/app.asar',
    'resources/backend/backend-1.0.0.jar', 'resources/runtime/bin/java.exe',
    'resources/web/index.html', 'resources/resources-manifest.json',
  ]) {
    if (!(await lstat(path.join(root, file))).isFile()) throw new Error(`Missing package resource: ${file}`);
  }
  const release = await readFile(path.join(root, 'resources/runtime/release'), 'utf8');
  if (!/JAVA_VERSION="25[."]/.test(release)) throw new Error('Package requires Java 25');
  const flag = await lstat(path.join(root, 'portable.flag')).catch((error) => {
    if (error.code === 'ENOENT') return null;
    throw error;
  });
  if (portable !== Boolean(flag?.isFile())) throw new Error('Incorrect portable.flag for package target');
  await inspectTree(root);
}

async function inspectTree(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (entry.isSymbolicLink()) throw new Error(`Package contains a link: ${entry.name}`);
    if (/^(user-data|runtime[.]json)$/i.test(entry.name)
      || /[.](db|sqlite|sqlite3)(-(wal|shm|journal))?$/i.test(entry.name)) {
      throw new Error(`Package contains mutable user data: ${entry.name}`);
    }
    if (entry.isDirectory()) await inspectTree(path.join(directory, entry.name));
  }
}

export default async function afterPack(context) {
  await verifyPackage(context.appOutDir, context.targets.some((target) => target.name === 'zip'));
}
