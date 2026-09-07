import { createServer, type Server } from 'node:http';
import { afterEach, describe, expect, it } from 'vitest';
import { waitForHealth } from '../../src/backend/healthClient';

let server: Server | null = null;

afterEach(async () => {
  if (!server) return;
  await new Promise<void>((resolve) => server?.close(() => resolve()));
  server = null;
});

describe('waitForHealth', () => {
  it('accepts the backend health envelope', async () => {
    server = createServer((_request, response) => {
      response.setHeader('Content-Type', 'application/json');
      response.end(JSON.stringify({ code: 200, data: { status: 'UP', version: '1.0.0' } }));
    });
    await new Promise<void>((resolve) => server?.listen(0, '127.0.0.1', resolve));
    const address = server.address();
    if (!address || typeof address === 'string') throw new Error('Expected an IP socket');

    await expect(waitForHealth(`http://127.0.0.1:${address.port}`, 1_000, 10))
      .resolves.toEqual({ status: 'UP', version: '1.0.0' });
  });

  it('times out when the payload is not healthy', async () => {
    server = createServer((_request, response) => {
      response.setHeader('Content-Type', 'application/json');
      response.end(JSON.stringify({ code: 200, data: { status: 'DOWN', version: '1.0.0' } }));
    });
    await new Promise<void>((resolve) => server?.listen(0, '127.0.0.1', resolve));
    const address = server.address();
    if (!address || typeof address === 'string') throw new Error('Expected an IP socket');

    await expect(waitForHealth(`http://127.0.0.1:${address.port}`, 80, 10))
      .rejects.toThrow('Backend health check timed out');
  });
});
