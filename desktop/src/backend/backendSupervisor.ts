import { randomUUID } from 'node:crypto';
import { createWriteStream, type WriteStream } from 'node:fs';
import { access, mkdir } from 'node:fs/promises';
import { constants } from 'node:fs';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { spawn, type ChildProcess } from 'node:child_process';
import { findAvailablePort } from './portResolver';
import { waitForHealth, type BackendHealth } from './healthClient';

export type BackendState = 'STOPPED' | 'STARTING' | 'READY' | 'STOPPING' | 'FAILED';

export type BackendLaunchOptions = {
  javaExecutable: string;
  backendJar: string;
  webRoot: string;
  dataRoot: string;
  databaseFile: string;
  logsDir: string;
  host?: string;
  preferredPort?: number;
  portAttempts?: number;
  startupTimeoutMs?: number;
  shutdownTimeoutMs?: number;
  environment?: NodeJS.ProcessEnv;
  onUnexpectedExit?: (error: Error) => void;
};

export type BackendRuntimeInfo = {
  baseUrl: string;
  host: string;
  port: number;
  health: BackendHealth;
};

export class BackendSupervisor {
  private state: BackendState = 'STOPPED';
  private child: ChildProcess | null = null;
  private logStream: WriteStream | null = null;
  private runtimeInfo: BackendRuntimeInfo | null = null;
  private readonly shutdownToken = randomUUID();

  constructor(private readonly options: BackendLaunchOptions) {}

  getState() {
    return this.state;
  }

  getRuntimeInfo() {
    return this.runtimeInfo;
  }

  async start(): Promise<BackendRuntimeInfo> {
    if (this.state !== 'STOPPED' && this.state !== 'FAILED') {
      throw new Error(`Cannot start backend while state is ${this.state}`);
    }
    this.state = 'STARTING';

    try {
      await this.prepareLaunchPaths();
      const host = this.options.host ?? '127.0.0.1';
      const port = await findAvailablePort(
        host,
        this.options.preferredPort ?? 17843,
        this.options.portAttempts ?? 20,
      );
      const baseUrl = `http://${host}:${port}`;
      this.logStream = createWriteStream(join(this.options.logsDir, 'backend.log'), { flags: 'a' });
      this.logStream.write(`\n[desktop] starting backend at ${new Date().toISOString()} on ${baseUrl}\n`);

      const child = spawn(this.options.javaExecutable, [
        '--enable-native-access=ALL-UNNAMED',
        '-jar',
        resolve(this.options.backendJar),
      ], {
        shell: false,
        windowsHide: true,
        cwd: resolve(this.options.dataRoot),
        env: this.backendEnvironment(host, port),
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      this.child = child;
      child.stdout?.pipe(this.logStream, { end: false });
      child.stderr?.pipe(this.logStream, { end: false });
      child.once('error', (error) => this.onBackendError(child, error));
      child.once('exit', (code, signal) => this.onBackendExit(child, code, signal));

      const health = await waitForBackendReady(child, baseUrl, this.options.startupTimeoutMs ?? 30_000);

      this.runtimeInfo = { baseUrl, host, port, health };
      this.state = 'READY';
      return this.runtimeInfo;
    } catch (error) {
      this.state = 'FAILED';
      await this.forceStopIfRunning();
      this.runtimeInfo = null;
      this.logStream?.end();
      this.logStream = null;
      throw error;
    }
  }

  async stop(): Promise<void> {
    const child = this.child;
    if (!child || child.exitCode !== null) {
      this.finishStoppedState();
      return;
    }

    this.state = 'STOPPING';
    await this.requestGracefulShutdown();
    if (!await waitForExit(child, this.options.shutdownTimeoutMs ?? 12_000)) {
      child.kill('SIGTERM');
      if (!await waitForExit(child, 3_000)) child.kill('SIGKILL');
    }
    this.finishStoppedState();
  }

  private async prepareLaunchPaths() {
    await Promise.all([
      access(this.options.backendJar, constants.R_OK),
      access(this.options.webRoot, constants.R_OK),
      mkdir(dirname(this.options.databaseFile), { recursive: true }),
      mkdir(this.options.logsDir, { recursive: true }),
    ]);
    if (isAbsolute(this.options.javaExecutable)) {
      await access(this.options.javaExecutable, constants.X_OK);
    }
  }

  private backendEnvironment(host: string, port: number): NodeJS.ProcessEnv {
    return {
      ...process.env,
      ...this.options.environment,
      SERVER_ADDRESS: host,
      SERVER_PORT: String(port),
      APP_DATA_ROOT: resolve(this.options.dataRoot),
      APP_DATA_DB_URL: toSqliteJdbcUrl(resolve(this.options.databaseFile)),
      APP_DICTIONARY_ENABLED: 'false',
      APP_DICTIONARY_DB_URL: 'jdbc:sqlite:file:dictionary-disabled?mode=memory&cache=shared',
      APP_WEB_ROOT: resolve(this.options.webRoot),
      APP_DESKTOP_MODE: 'true',
      APP_DESKTOP_SHUTDOWN_TOKEN: this.shutdownToken,
    };
  }

  private async requestGracefulShutdown() {
    if (!this.runtimeInfo) return;
    try {
      await fetch(`${this.runtimeInfo.baseUrl}/api/desktop/shutdown`, {
        method: 'POST',
        headers: { 'X-Desktop-Shutdown-Token': this.shutdownToken },
        signal: AbortSignal.timeout(2_000),
      });
    } catch (error) {
      this.logStream?.write(`[desktop] graceful shutdown request failed: ${formatError(error)}\n`);
    }
  }

  private async forceStopIfRunning() {
    const child = this.child;
    if (!child || child.exitCode !== null) return;
    child.kill('SIGTERM');
    if (!await waitForExit(child, 3_000)) child.kill('SIGKILL');
  }

  private onBackendExit(child: ChildProcess, code: number | null, signal: NodeJS.Signals | null) {
    const unexpectedExit = this.state !== 'STOPPING' && this.runtimeInfo !== null;
    this.logStream?.write(`[desktop] backend exited with code=${String(code)} signal=${String(signal)}\n`);
    this.logStream?.end();
    this.logStream = null;
    if (this.child === child) this.child = null;
    if (this.state !== 'STOPPING') this.state = code === 0 ? 'STOPPED' : 'FAILED';
    this.runtimeInfo = null;
    if (unexpectedExit) {
      this.options.onUnexpectedExit?.(
        new Error(`Backend exited unexpectedly with code=${String(code)} signal=${String(signal)}`),
      );
    }
  }

  private onBackendError(child: ChildProcess, error: Error) {
    const unexpectedExit = this.runtimeInfo !== null;
    this.logStream?.write(`[desktop] backend process error: ${error.message}\n`);
    if (this.child === child) this.child = null;
    this.state = 'FAILED';
    this.runtimeInfo = null;
    if (unexpectedExit) this.options.onUnexpectedExit?.(error);
  }

  private finishStoppedState() {
    this.child = null;
    this.runtimeInfo = null;
    this.state = 'STOPPED';
    this.logStream?.end();
    this.logStream = null;
  }
}

function toSqliteJdbcUrl(databaseFile: string) {
  return `jdbc:sqlite:${databaseFile.replaceAll('\\', '/')}`;
}

async function waitForExit(child: ChildProcess, timeoutMs: number): Promise<boolean> {
  if (child.exitCode !== null) return true;
  return new Promise((resolveExit) => {
    const timer = setTimeout(() => {
      child.removeListener('exit', onExit);
      resolveExit(false);
    }, timeoutMs);
    timer.unref();
    const onExit = () => {
      clearTimeout(timer);
      resolveExit(true);
    };
    child.once('exit', onExit);
  });
}

function waitForBackendReady(child: ChildProcess, baseUrl: string, timeoutMs: number): Promise<BackendHealth> {
  return new Promise((resolveReady, rejectReady) => {
    const cleanup = () => {
      child.removeListener('error', onError);
      child.removeListener('exit', onExit);
    };
    const onError = (error: Error) => {
      cleanup();
      rejectReady(new Error(`Backend process failed to start: ${error.message}`));
    };
    const onExit = (code: number | null, signal: NodeJS.Signals | null) => {
      cleanup();
      rejectReady(new Error(`Backend exited during startup with code=${String(code)} signal=${String(signal)}`));
    };
    child.once('error', onError);
    child.once('exit', onExit);
    void waitForHealth(baseUrl, timeoutMs).then((health) => {
      cleanup();
      resolveReady(health);
    }, (error: unknown) => {
      cleanup();
      rejectReady(error);
    });
  });
}

function formatError(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}
