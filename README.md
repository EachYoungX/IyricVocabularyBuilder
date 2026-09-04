# Lyric Vocabulary Builder

[English](README.md) | [简体中文](README.zh-CN.md)

Lyric Vocabulary Builder is a local-first vocabulary learning app for Chinese-speaking English learners. It is not a lyrics player and does not ship a lyrics library. Instead, it turns user-provided English lyrics into structured, searchable, reviewable vocabulary study material.

The project focuses on one practical learning loop: import lyrics you are allowed to use, clean and structure them, search words in real lyric context, check bilingual dictionary entries, and track personal vocabulary status.

## Screenshots

![Vocabulary workspace](assets/screenshots/vocabulary-workspace.png)

![Songs manager](assets/screenshots/songs-manager.png)

## Highlights

- **Structured lyric import**: TXT, JSON, LRC, SRT, and manual paste workflows are designed for learning material preparation.
- **Recoverable cleanup**: the app keeps raw lyrics, normalized lyrics, line classification, and user corrections instead of silently deleting context.
- **Lemma-based search**: related forms such as `running`, `ran`, and `runs` can be grouped under `run`.
- **Vocabulary in context**: words are reviewed inside the lyric lines where they actually appear.
- **Optional external dictionary integration**: dictionary files are supplied by the runtime environment; this project never bundles a dictionary and also supports no-dictionary mode.
- **Personal vocabulary loop**: learners can add words, update status, track familiarity, view stats, and review pending words.
- **Chinese learner friendly**: product copy, edge cases, and documentation are written around Chinese-native learners studying English through songs.

## What The App Does

The learning flow is intentionally linear:

1. Import lyrics from TXT, JSON, LRC, SRT, or manual paste.
2. Normalize the text and preserve both the raw import and the learning version.
3. Review or edit the learning text without destroying the original import.
4. Tokenize lyric lines, normalize word forms, and build a lemma-based vocabulary index.
5. Look up words in their real lyric context with bilingual dictionary support.
6. Add useful words to the personal vocabulary list and update learning status over time.

## Tech Stack

| Layer | Stack |
|---|---|
| Frontend | Vue 3, Quasar 2, TypeScript, Pinia, Vue Router, Axios |
| Backend | Java 25, Spring Boot 3.5, Spring Web, Spring Data JPA |
| Database | SQLite user database + optional external SQLite dictionary |
| API Contract | OpenAPI 3.1, generated TypeScript client |
| Tooling | Maven Wrapper, pnpm, ESLint, Vite |

## Architecture

At a high level, the app is split into four parts:

- The learner uses a Vue and Quasar frontend for importing songs, browsing vocabulary, managing lyrics, and tracking personal word status.
- The frontend talks to a Spring Boot API through an OpenAPI-generated TypeScript client.
- The backend stores songs, structured lyric lines, tokenized vocabulary, and personal learning records in a local SQLite user database.
- The vocabulary index builder derives searchable lemma entries from the user's imported lyrics. Dictionary lookup is enabled only when the runtime supplies an external SQLite file; this project never packages dictionary data.

## Local Development

Requirements: Java 25, Node.js 20 or newer, and pnpm 11. The repository includes the Maven Wrapper, so a separate Maven installation is not required.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Default API URL:

```text
http://localhost:8080
```

The default configuration uses a local SQLite database:

```text
backend/data/app_data.db
```

If the database does not exist, the backend initializes it from `schema.sql` and applies the required lightweight migrations. Dictionary integration is disabled by default. In this mode, song import, lyric editing, vocabulary indexing, and personal vocabulary remain available; dictionary definitions and dictionary-backed phrase matching return empty/not-found results without accessing dictionary tables.

Optional dictionary override:

```text
APP_DICTIONARY_ENABLED=true
APP_DICTIONARY_DB_URL=jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite
```

For an explicit no-dictionary local run:

```bash
APP_DICTIONARY_ENABLED=false ./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Quasar prints the dev URL in the terminal. It is usually:

```text
http://localhost:9000
```

### Generate The API Client

The backend OpenAPI contract lives at:

```text
backend/src/main/resources/api-docs.yaml
```

Generate the frontend client:

```powershell
cd frontend
pnpm gen-api
```

The post-generation script adapts the generated client to the backend response envelope.

Development defaults to `http://localhost:8080`. Set `VITE_API_BASE_URL` when the API is on another host. Production builds use same-origin `/api` calls by default, which is suited to a reverse proxy. For LAN development, add the exact frontend origins to the backend, for example `APP_CORS_ALLOWED_ORIGINS=http://192.168.1.20:9000`, and point the frontend at the reachable backend address.

## Verification

Backend:

```bash
cd backend
./mvnw clean test
```

Frontend:

```powershell
cd frontend
pnpm lint
pnpm test
pnpm build
```

## Deployment Notes

The current project is best suited for local learning, demos, or a small self-hosted deployment.

1. Build and run the backend:

```bash
cd backend
./mvnw clean package
java -jar target/backend-1.0.0.jar
```

2. Build the frontend:

```powershell
cd frontend
pnpm build
```

3. Serve `frontend/dist/spa` as a static site and proxy API requests to the backend.

The default deployment target is localhost or a trusted LAN. The backend has no user authentication or authorization, and CORS is not an authentication mechanism, so the API should not be exposed directly to the public internet. Put authentication and TLS in a trusted reverse proxy if broader access is required. Keep `backend/data/app_data.db` out of git, avoid publishing user-imported lyrics, and keep dictionary files outside release artifacts.

## Backup And Restore

The Data Management page exports a versioned full backup containing source songs, structured lyrics and user corrections, credits, personal vocabulary, user phrases, and persistent vocabulary overrides. Restore validates the complete backup before mutation and supports transactional overwrite restore. Derived vocabulary and phrase caches are rebuilt after restore.

## Data And Copyright Boundary

- This repository does not include or distribute a lyrics database.
- Users should only import lyrics they are allowed to use or process.
- Imported lyrics, cleanup results, vocabulary indexes, and personal learning state are stored locally.
- The independent `LyricVocabularyDictionary` project maintains dictionary source data and releases; this project does not include or distribute dictionary files. Users who provide an external dictionary are responsible for verifying its source, license, permissions, and compliance.
- This project makes no warranty about the accuracy, completeness, authorization, or suitability of externally supplied dictionary data.

## Repository Layout

```text
.
├── backend/                 # Spring Boot API
│   ├── src/main/java/       # domain code
│   ├── src/main/resources/  # schema, OpenAPI, runtime configuration (no dictionary)
│   └── data/                # local runtime database, ignored by git
├── frontend/                # Quasar Vue app
│   ├── src/components/
│   ├── src/pages/
│   ├── src/services/api/    # generated OpenAPI client
│   └── src/css/             # visual tokens and global styles
├── assets/screenshots/      # README screenshots
├── CHANGELOG.md             # completed and verified stages
└── README.zh-CN.md          # Chinese README
```

2ndLA source data, processing JSON, translation batches, and phrase release files are maintained outside this repository. The dictionary project and this music/lyrics project are released independently; this project only connects to an external SQLite file through `APP_DICTIONARY_DB_URL`.

## Status

Stages 0 through 7 have been completed and verified. See [CHANGELOG.md](CHANGELOG.md) for the refactoring history.
