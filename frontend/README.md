# Lyric Vocabulary Builder Frontend

Quasar + Vue 3 + TypeScript 前端，用于英文歌词词汇学习、歌曲管理、歌词结构化预览、词典查询和个人词库复习。

This frontend is the browser UI for importing English lyrics, exploring vocabulary in song context, checking dictionary entries, and tracking a personal review list.

## Stack

- Quasar 2
- Vue 3
- TypeScript
- Pinia
- Vue Router
- Vue I18n
- Axios
- OpenAPI TypeScript Codegen

## Key Screens

- `IndexPage.vue`: vocabulary workspace, occurrence list, dictionary panel, personal vocabulary stats, review queue.
- `SongsManagerPage.vue`: songs library, edit form, batch delete, structured lyric dialog.
- Dictionary data is supplied externally by the runtime; the frontend does not expose or package dictionary source metadata.
- `SongImportDialog.vue`: JSON/TXT/LRC/SRT import, import preview, lyric summary, async import progress.
- `LyricStructureDialog.vue`: raw/normalized lyric comparison and per-line correction.

## Visual System

The UI uses the `Midnight Sail` palette:

```text
#1B3C53  #456882  #D2C1B6  #F9F3EF
```

Global design tokens live in:

```text
src/css/app.scss
src/css/quasar.variables.scss
```

The app keeps a quiet paper-white interface with restrained serif display headings, so the lyric and vocabulary content stays central.

## Generated API Client

The canonical API contract lives in the backend:

```text
../backend/src/main/resources/api-docs.yaml
```

Generate the frontend client:

```powershell
pnpm gen-api
```

Generated files are under:

```text
src/services/api/
```

The post-generation patch unwraps backend `{ code, message, data }` envelopes and keeps browser-native `FormData` compatibility.

## Local Development

```powershell
pnpm install
pnpm dev
```

Backend API is expected at:

```text
http://localhost:8080
```

## Build And Check

```powershell
pnpm lint
pnpm build
```

The production build is emitted to:

```text
dist/spa
```

## Project Structure

```text
frontend/
├── src/
│   ├── boot/              # i18n bootstrapping
│   ├── components/        # import and lyric dialogs
│   ├── composables/       # shared page flows
│   ├── css/               # visual tokens and global styles
│   ├── i18n/              # zh-CN and en-US text
│   ├── layouts/           # app shell
│   ├── pages/             # route pages
│   ├── router/            # routes
│   ├── services/api/      # generated OpenAPI client
│   ├── stores/            # Pinia stores
│   └── utils/             # import parsing and helpers
└── package.json
```
