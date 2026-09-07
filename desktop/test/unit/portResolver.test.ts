import { createServer } from 'node:net';
import { afterEach, describe, expect, it } from 'vitest';
import { findAvailablePort } from '../../src/backend/portResolver';

const openServers: ReturnType<typeof createServer>[] = [];

afterEach(async () => {
  await Promise.all(openServers.splice(0).map((server) => new Promise<void>((resolve) => server.close(() => resolve()))));
});

describe('findAvailablePort', () => {
  it('skips a port that is already occupied', async () => {
    const occupied = createServer();
    openServers.push(occupied);
    await new Promise<void>((resolve) => occupied.listen(0, '127.0.0.1', resolve));
    const address = occupied.address();
    if (!address || typeof address === 'string') throw new Error('Expected an IP socket');

    const selected = await findAvailablePort('127.0.0.1', address.port, 10);

    expect(selected).toBeGreaterThan(address.port);
    expect(selected).toBeLessThan(address.port + 10);
  });

  it('rejects an invalid search range', async () => {
    await expect(findAvailablePort('127.0.0.1', 0, 1)).rejects.toThrow('Invalid backend port search range');
  });
});
