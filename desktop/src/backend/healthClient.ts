export type BackendHealth = {
  status: 'UP';
  version: string;
};

type HealthEnvelope = {
  data?: Partial<BackendHealth>;
};

export async function waitForHealth(
  baseUrl: string,
  timeoutMs = 30_000,
  intervalMs = 200,
): Promise<BackendHealth> {
  const deadline = Date.now() + timeoutMs;
  let lastError: unknown;

  while (Date.now() < deadline) {
    try {
      const remaining = deadline - Date.now();
      const response = await fetch(`${baseUrl}/api/health`, {
        signal: AbortSignal.timeout(Math.max(1, Math.min(1_000, remaining))),
      });
      if (response.ok) {
        const envelope = await response.json() as HealthEnvelope;
        if (envelope.data?.status === 'UP' && typeof envelope.data.version === 'string') {
          return { status: 'UP', version: envelope.data.version };
        }
      }
      lastError = new Error(`Health endpoint returned HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await delay(Math.min(intervalMs, Math.max(0, deadline - Date.now())));
  }

  const detail = lastError instanceof Error ? `: ${lastError.message}` : '';
  throw new Error(`Backend health check timed out after ${timeoutMs} ms${detail}`);
}

function delay(milliseconds: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, milliseconds));
}
