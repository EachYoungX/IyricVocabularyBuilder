import { chmod, mkdir, readFile, rename, rm, stat } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const desktopRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const stageRoot = path.join(desktopRoot, '.stage');
const runtimeRoot = path.join(stageRoot, 'runtime');
const runtimeNext = path.join(stageRoot, 'runtime.next');
const jdkHome = process.env.LVB_WINDOWS_JDK_HOME?.trim();

if (!jdkHome) {
  throw new Error(
    'LVB_WINDOWS_JDK_HOME must point to an extracted Microsoft OpenJDK 25 Windows x64 archive',
  );
}

const resolvedJdkHome = path.resolve(jdkHome);
const jlinkExecutable = path.join(resolvedJdkHome, 'bin', 'jlink.exe');
const modulePath = path.join(resolvedJdkHome, 'jmods');
await requireFile(jlinkExecutable, 'Windows jlink executable');
await requireDirectory(modulePath, 'Windows JDK jmods directory');
if (process.platform !== 'win32') await chmod(jlinkExecutable, 0o755);

const jlinkVersion = (await runAndCapture(jlinkExecutable, ['--version'])).trim();
if (!/^25(?:\.|$)/.test(jlinkVersion)) {
  throw new Error(`Windows JDK 25 is required; jlink reported ${jlinkVersion}`);
}

await mkdir(stageRoot, { recursive: true });
await rm(runtimeNext, { recursive: true, force: true });
await run(jlinkExecutable, [
  '--module-path', modulePath,
  '--add-modules', [
    'java.base',
    'java.desktop',
    'java.instrument',
    'java.logging',
    'java.management',
    'java.naming',
    'java.net.http',
    'java.prefs',
    'java.rmi',
    'java.scripting',
    'java.security.jgss',
    'java.security.sasl',
    'java.sql',
    'java.transaction.xa',
    'java.xml',
    'jdk.crypto.cryptoki',
    'jdk.crypto.ec',
    'jdk.management',
    'jdk.unsupported',
    'jdk.zipfs',
  ].join(','),
  '--strip-debug',
  '--no-header-files',
  '--no-man-pages',
  '--compress=zip-6',
  '--output', runtimeNext,
]);

const javaExecutable = path.join(runtimeNext, 'bin', 'java.exe');
await requireFile(javaExecutable, 'generated Java executable');
if (process.platform !== 'win32') await chmod(javaExecutable, 0o755);
const release = await readFile(path.join(runtimeNext, 'release'), 'utf8');
if (!release.includes('JAVA_VERSION="25.')) {
  throw new Error('Generated runtime does not identify itself as Java 25');
}
await rm(runtimeRoot, { recursive: true, force: true });
await rename(runtimeNext, runtimeRoot);
process.stdout.write(`Created Windows Java ${jlinkVersion} runtime at ${runtimeRoot}\n`);

async function requireFile(candidate, label) {
  const info = await stat(candidate).catch(() => null);
  if (!info?.isFile()) throw new Error(`${label} not found: ${candidate}`);
}

async function requireDirectory(candidate, label) {
  const info = await stat(candidate).catch(() => null);
  if (!info?.isDirectory()) throw new Error(`${label} not found: ${candidate}`);
}

function run(executable, args) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(executable, args, { shell: false, stdio: 'inherit' });
    child.once('error', rejectRun);
    child.once('exit', (code, signal) => {
      if (code === 0) resolveRun();
      else rejectRun(new Error(`${executable} exited with code=${String(code)} signal=${String(signal)}`));
    });
  });
}

function runAndCapture(executable, args) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(executable, args, { shell: false, stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.once('error', rejectRun);
    child.once('exit', (code, signal) => {
      if (code === 0) resolveRun(stdout || stderr);
      else rejectRun(new Error(`${executable} exited with code=${String(code)} signal=${String(signal)}: ${stderr}`));
    });
  });
}
