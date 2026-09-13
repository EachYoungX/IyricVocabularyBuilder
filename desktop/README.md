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

Development mode uses the system `java` executable. All mutable state is isolated under `<repository>/.desktop-dev/`, including the application database, runtime configuration, logs, cache, temporary files, and Electron profile/session data. Production builds use the bundled Java 25 runtime.

## Data modes

- An installed build stores all application-managed state under `%LOCALAPPDATA%\LyricVocabularyBuilder`.
- A packaged build becomes portable when `portable.flag` exists beside its executable. Its fixed data root is `<exeDir>\\user-data`; startup fails clearly if that directory is not writable.
- Installed and portable modes never fall back to one another. Moving data between them uses the application's complete Backup/Restore flow.

The shell owns `<data-root>/config/runtime.json`. The schema-versioned file records whether the dictionary dataset is managed by the application or selected from an absolute external path. Managed datasets store only a file name so a portable directory remains movable. Configuration updates use a temporary file followed by an atomic rename.

## Dictionary datasets

The Settings page scans only the application-managed `datasets` directory. A single compatible managed file is selected automatically; multiple compatible files require an explicit choice. Import first copies into `datasets/.incoming/*.partial`, validates the closed copy through the backend probe mode, and then atomically renames it into place.

An external dataset is referenced in place and is never copied, moved, or deleted by the application. If it disappears, startup continues in no-dictionary mode. Dataset switches persist configuration, gracefully restart the backend, wait for health, and reload the renderer; a failed switch restores the previous configuration and backend.

SQLite table, column, metadata, schema-version, and package-version compatibility is implemented once in the backend. The desktop shell invokes the same backend JAR with `APP_DICTIONARY_PROBE_ONLY=true` instead of duplicating these rules in TypeScript. Active dictionary connections use SQLite read-only mode.

## Local network access

The backend binds `127.0.0.1` by default. The Settings page can explicitly enable LAN access, which persists `lan.enabled`, restarts the backend on `0.0.0.0`, and displays the host's non-loopback IPv4 URLs. The desktop window continues to use its loopback URL. A failed bind restores the previous setting and backend.

LAN browsers receive the same SPA and HTTP API but no `desktopBridge`, host paths, file pickers, managed-dataset deletion, or other native capabilities. The backend has no user authentication, so the UI warns that LAN mode is only for trusted private networks.

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

## Windows release artifacts

After building the backend, frontend and runtime:

```bash
pnpm install --frozen-lockfile
pnpm package:win:setup
pnpm package:win:portable
```

Outputs in desktop/release:

- LyricVocabularyBuilder-Setup-1.0.0.exe: assisted, fixed-directory, per-user NSIS installation.
- LyricVocabularyBuilder-Portable-1.0.0.zip: full application with portable.flag beside the executable.

The portable configuration extends the common resource configuration and uses electron-builder's ZIP target. Each build creates a fresh unpacked tree. The afterPack hook checks required resources, Java 25, portable mode identity, and rejects bundled databases, user-data, runtime.json and symlinks. Build the targets sequentially because they share the staging and unpacked directories.

The NSIS include is tracked source under build/. Uninstall keeps data by default; explicit selection and confirmation remove only the fixed LocalAppData application directory. Silent upgrade/uninstall preserves user data.

Build NSIS on native Windows with Node and dependencies installed on a local Windows filesystem. Linux/WSL NSIS builds additionally require a working Wine installation. The directory and ZIP targets also build in WSL. When copying a WSL project to Windows for packaging, install dependencies there with pnpm install --frozen-lockfile; WSL node_modules symlinks are platform-specific.

Code signing uses electron-builder's standard CSC_LINK and CSC_KEY_PASSWORD build environment variables when a certificate is supplied. Keep certificate material outside the repository. Unsigned builds remain unsigned and require normal Windows security handling.

## Desktop reset

The desktop data reset invokes the existing backend transaction, stops the backend, resets runtime settings (including LAN), clears Electron browser storage and cache, and cleans application cache/temp directories. It then starts the backend and reloads the window. Managed dictionary files and external files are retained. Java temporary files, including SQLite native libraries, use the mode-specific temp directory.

See [Windows user guide](../docs/desktop/windows-guide.md) for installation, datasets, backup and uninstall behavior.
