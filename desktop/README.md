# Lyric Vocabulary Builder Desktop

This directory contains the Electron shell for the Windows desktop edition. The shell owns process lifecycle and native integration; application business logic remains in the Spring Boot backend and Vue frontend.

## Phase 1 development

Build the production web application and backend JAR first:

```bash
cd ../frontend
pnpm build

cd ../backend
./mvnw clean package
```

Install and run the desktop shell:

```bash
cd ../desktop
pnpm install
pnpm dev
```

Development mode uses the system `java` executable and stores the application database and backend log under `<repository>/.desktop-dev/`. Production builds will use the bundled runtime introduced in Desktop Phase 3.

## Verification

```bash
pnpm typecheck
pnpm lint
pnpm test
pnpm build
pnpm test:integration
```

The integration test starts the packaged Spring Boot JAR, waits for `/api/health`, verifies the externally served Vue SPA, requests graceful shutdown, and confirms the database pools close normally.

## Security boundary

- Renderer Node integration is disabled.
- Context isolation and Chromium sandboxing are enabled.
- Preload exposes only the declared `desktopBridge` methods.
- Java starts through `spawn()` with an argument array and `shell: false`.
- The backend shutdown endpoint exists only in desktop mode and requires a per-process random token.
