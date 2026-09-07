import { copyFile, cp, mkdir, readFile, rm, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const desktopRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repositoryRoot = path.resolve(desktopRoot, '..');
const stageRoot = path.join(desktopRoot, '.stage');
const backendSource = path.join(repositoryRoot, 'backend', 'target', 'backend-1.0.0.jar');
const webSource = path.join(repositoryRoot, 'frontend', 'dist', 'spa');
const runtimeJava = path.join(stageRoot, 'runtime', 'bin', 'java.exe');
const backendStage = path.join(stageRoot, 'backend');
const webStage = path.join(stageRoot, 'web');

await requireFile(backendSource, 'Backend JAR; run the backend package build first');
await requireFile(path.join(webSource, 'index.html'), 'Vue production index; run the frontend build first');
await requireFile(runtimeJava, 'Bundled Java runtime; run pnpm runtime:build first');

await rm(backendStage, { recursive: true, force: true });
await rm(webStage, { recursive: true, force: true });
await mkdir(backendStage, { recursive: true });
await copyFile(backendSource, path.join(backendStage, 'backend-1.0.0.jar'));
await cp(webSource, webStage, { recursive: true });

const runtimeRelease = await readFile(path.join(stageRoot, 'runtime', 'release'), 'utf8');
const javaVersion = /JAVA_VERSION="([^"]+)"/.exec(runtimeRelease)?.[1];
if (!javaVersion?.startsWith('25.')) throw new Error('Staged runtime must be Java 25');

const manifest = {
  schemaVersion: 1,
  javaVersion,
  backend: 'backend/backend-1.0.0.jar',
  web: 'web/index.html',
  runtime: 'runtime/bin/java.exe',
};
await writeFile(
  path.join(stageRoot, 'resources-manifest.json'),
  `${JSON.stringify(manifest, null, 2)}\n`,
  'utf8',
);
process.stdout.write(`Staged production resources at ${stageRoot}\n`);

async function requireFile(candidate, label) {
  const info = await stat(candidate).catch(() => null);
  if (!info?.isFile()) throw new Error(`${label}: ${candidate}`);
}
