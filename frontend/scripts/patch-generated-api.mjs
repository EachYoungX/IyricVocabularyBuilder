import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const requestPath = fileURLToPath(
  new URL('../src/services/api/core/request.ts', import.meta.url),
);
const openApiPath = fileURLToPath(
  new URL('../src/services/api/core/OpenAPI.ts', import.meta.url),
);
const original = await readFile(requestPath, 'utf8');
const generatedLine = '        return response.data;';
const formDataImport = "import FormData from 'form-data';\n";
const formHeadersLine = "    const formHeaders = typeof formData?.getHeaders === 'function' && formData?.getHeaders() || {}\n";
const browserFormHeadersBlock = `    const formHeaders = typeof (formData as FormData & { getHeaders?: () => Record<string, string> } | undefined)?.getHeaders === 'function'
        ? (formData as FormData & { getHeaders: () => Record<string, string> }).getHeaders()
        : {};
`;
const envelopeAwareBlock = `        const body = response.data;
        if (body && typeof body === 'object' && 'code' in body && 'message' in body && 'data' in body) {
            return body.data;
        }
        return body;`;

if (!original.includes(generatedLine)) {
  throw new Error('Generated request.ts has changed; response envelope patch was not applied.');
}

await writeFile(
  requestPath,
  original
    .replace(formDataImport, '')
    .replace(formHeadersLine, browserFormHeadersBlock)
    .replace(generatedLine, envelopeAwareBlock),
  'utf8',
);

const openApi = await readFile(openApiPath, 'utf8');
const generatedBase = "    BASE: 'http://localhost:8080',";
const environmentBase = "    BASE: import.meta.env.VITE_API_BASE_URL?.replace(/\\\/$/, '') || (import.meta.env.DEV ? 'http://localhost:8080' : ''),";
if (!openApi.includes(generatedBase)) {
  throw new Error('Generated OpenAPI.ts base URL changed; environment patch was not applied.');
}
await writeFile(openApiPath, openApi.replace(generatedBase, environmentBase), 'utf8');
