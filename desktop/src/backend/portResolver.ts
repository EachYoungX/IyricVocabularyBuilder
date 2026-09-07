import { createServer } from 'node:net';

export async function findAvailablePort(
  host = '127.0.0.1',
  preferredPort = 17843,
  attempts = 20,
): Promise<number> {
  if (preferredPort < 1 || preferredPort > 65535 || attempts < 1) {
    throw new Error('Invalid backend port search range');
  }

  for (let offset = 0; offset < attempts; offset += 1) {
    const candidate = preferredPort + offset;
    if (candidate > 65535) break;
    if (await canBind(host, candidate)) return candidate;
  }
  throw new Error(`No available backend port in ${preferredPort}-${Math.min(65535, preferredPort + attempts - 1)}`);
}

function canBind(host: string, port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const server = createServer();
    server.unref();
    server.once('error', () => resolve(false));
    server.listen(port, host, () => server.close(() => resolve(true)));
  });
}
