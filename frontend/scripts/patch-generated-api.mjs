import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const requestPath = fileURLToPath(
  new URL('../src/services/api/core/request.ts', import.meta.url),
);
const original = await readFile(requestPath, 'utf8');
const generatedLine = '        return response.data;';
const envelopeAwareBlock = `        const body = response.data;
        if (body && typeof body === 'object' && 'code' in body && 'message' in body && 'data' in body) {
            return body.data;
        }
        return body;`;

if (!original.includes(generatedLine)) {
  throw new Error('Generated request.ts has changed; response envelope patch was not applied.');
}

await writeFile(requestPath, original.replace(generatedLine, envelopeAwareBlock), 'utf8');
