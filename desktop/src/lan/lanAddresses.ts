import { networkInterfaces, type NetworkInterfaceInfo } from 'node:os';

type InterfaceMap = NodeJS.Dict<NetworkInterfaceInfo[]>;

export function lanUrlsForPort(
  port: number,
  interfaces: InterfaceMap = networkInterfaces(),
) {
  const addresses = Object.values(interfaces)
    .flatMap((entries) => entries ?? [])
    .filter((entry) => entry.family === 'IPv4' && !entry.internal)
    .map((entry) => entry.address)
    .filter((address) => address !== '0.0.0.0');
  return [...new Set(addresses)]
    .sort((left, right) => left.localeCompare(right))
    .map((address) => `http://${address}:${port}`);
}
