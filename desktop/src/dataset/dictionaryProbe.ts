import { spawn } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

export type DictionaryProbeResult = {
  status: 'valid' | 'invalid' | 'incompatible';
  schemaVersion?: string;
  datasetVersion?: string;
  message?: string;
};

export type DictionaryProbeOptions = {
  javaExecutable: string;
  backendJar: string;
  timeoutMs?: number;
  environment?: NodeJS.ProcessEnv;
};

export class DictionaryProbe {
  constructor(private readonly options: DictionaryProbeOptions) {}

  probe(candidate: string): Promise<DictionaryProbeResult> {
    const jdbcUrl = sqliteReadOnlyJdbcUrl(candidate);
    return new Promise((resolveProbe, rejectProbe) => {
      const child = spawn(this.options.javaExecutable, [
        '--enable-native-access=ALL-UNNAMED',
        '-jar',
        resolve(this.options.backendJar),
      ], {
        shell: false,
        windowsHide: true,
        env: {
          ...process.env,
          ...this.options.environment,
          APP_DICTIONARY_PROBE_ONLY: 'true',
          APP_DICTIONARY_DB_URL: jdbcUrl,
        },
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      let stdout = '';
      let stderr = '';
      let settled = false;
      child.stdout?.setEncoding('utf8');
      child.stderr?.setEncoding('utf8');
      child.stdout?.on('data', (chunk: string) => { stdout = appendBounded(stdout, chunk); });
      child.stderr?.on('data', (chunk: string) => { stderr = appendBounded(stderr, chunk); });
      const timer = setTimeout(() => {
        if (settled) return;
        settled = true;
        child.kill('SIGKILL');
        rejectProbe(new Error(`Dictionary probe timed out for ${candidate}`));
      }, this.options.timeoutMs ?? 30_000);
      timer.unref();
      child.once('error', (error) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        rejectProbe(new Error(`Dictionary probe could not start: ${error.message}`, { cause: error }));
      });
      child.once('exit', (code, signal) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        try {
          const result = parseProbeOutput(code === 0 ? stdout : stderr || stdout);
          if (code === 0 && result.status !== 'valid') {
            throw new Error(`Dictionary probe returned ${result.status} with exit code 0`);
          }
          resolveProbe(result);
        } catch (error) {
          rejectProbe(new Error(
            `Dictionary probe exited with code=${String(code)} signal=${String(signal)}: ${stderr || stdout}`,
            { cause: error },
          ));
        }
      });
    });
  }
}

export function sqliteReadOnlyJdbcUrl(candidate: string) {
  return `jdbc:sqlite:${pathToFileURL(resolve(candidate)).href}?mode=ro`;
}

function parseProbeOutput(output: string): DictionaryProbeResult {
  const jsonLine = output.trim().split(/\r?\n/).reverse()
    .find((line) => line.trim().startsWith('{'));
  if (!jsonLine) throw new Error('Dictionary probe returned no JSON result');
  const value: unknown = JSON.parse(jsonLine);
  if (!isRecord(value) || !isStatus(value.status)) throw new Error('Dictionary probe result is malformed');
  return {
    status: value.status,
    schemaVersion: stringOrUndefined(value.schemaVersion),
    datasetVersion: stringOrUndefined(value.datasetVersion),
    message: stringOrUndefined(value.message),
  };
}

function appendBounded(current: string, chunk: string) {
  const combined = current + chunk;
  return combined.length <= 64_000 ? combined : combined.slice(-64_000);
}

function stringOrUndefined(value: unknown) {
  return typeof value === 'string' ? value : undefined;
}

function isStatus(value: unknown): value is DictionaryProbeResult['status'] {
  return value === 'valid' || value === 'invalid' || value === 'incompatible';
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
