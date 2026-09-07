# Lyric Vocabulary Builder Desktop

This directory contains the Electron shell for the Windows desktop edition. The shell owns process lifecycle and native integration; application business logic remains in the Spring Boot backend and Vue frontend.

## Development

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

Development mode uses the system `java` executable. All mutable state is isolated under `<repository>/.desktop-dev/`, including the application database, runtime configuration, logs, cache, temporary files, and Electron profile/session data. Production builds will use the bundled runtime introduced in Desktop Phase 3.

## Data modes

- An installed build stores all application-managed state under `%LOCALAPPDATA%\LyricVocabularyBuilder`.
- A packaged build becomes portable when `portable.flag` exists beside its executable. Its fixed data root is `<exeDir>\\user-data`; startup fails clearly if that directory is not writable.
- Installed and portable modes never fall back to one another. Moving data between them uses the application's complete Backup/Restore flow.

The shell owns `<data-root>/config/runtime.json`. The schema-versioned file records whether the dictionary dataset is managed by the application or selected from an absolute external path. Managed datasets store only a file name so a portable directory remains movable. Configuration updates use a temporary file followed by an atomic rename.

## Windows production resources

Production builds bundle a Java 25 runtime generated with `jlink`; they never resolve Java from `JAVA_HOME` or `PATH`. Download and extract the Microsoft OpenJDK 25 Windows x64 ZIP, then provide its directory explicitly:

```bash
export LVB_WINDOWS_JDK_HOME=/path/to/extracted/windows-jdk-25
pnpm runtime:build
```

Build the backend and web application, then create an unpacked Windows application:

```bash
cd ../backend
./mvnw clean package

cd ../frontend
pnpm build

cd ../desktop
pnpm package:win:dir
```

The output is `desktop/release/win-unpacked`. Its `resources` directory contains `runtime/bin/java.exe`, `backend/backend-1.0.0.jar`, the Vue production files under `web`, and a resource manifest. Generated `.stage` and `release` directories are local build artifacts and are not committed.

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
