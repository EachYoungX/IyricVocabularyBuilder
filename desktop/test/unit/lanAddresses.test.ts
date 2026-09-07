import { describe, expect, it } from 'vitest';
import { lanUrlsForPort } from '../../src/lan/lanAddresses';

describe('lanUrlsForPort', () => {
  it('returns sorted unique non-internal IPv4 URLs', () => {
    const interfaces = {
      Ethernet: [
        { family: 'IPv4', internal: false, address: '192.168.1.8' },
        { family: 'IPv6', internal: false, address: 'fe80::1' },
      ],
      WiFi: [
        { family: 'IPv4', internal: false, address: '10.0.0.4' },
        { family: 'IPv4', internal: false, address: '192.168.1.8' },
      ],
      Loopback: [{ family: 'IPv4', internal: true, address: '127.0.0.1' }],
    } as never;

    expect(lanUrlsForPort(17843, interfaces)).toEqual([
      'http://10.0.0.4:17843',
      'http://192.168.1.8:17843',
    ]);
  });
});
