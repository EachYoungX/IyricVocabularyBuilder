# Lyric Vocabulary Builder Backend

Spring Boot 后端服务，负责歌曲库、歌词结构化、词汇索引、个人词库、复习队列和离线词典查询。

This backend turns user-provided English lyrics into normalized lyric lines, lemma-based vocabulary indexes, dictionary lookup data, and personal review state.

## Stack

- Java 25
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- SQLite
- OpenAPI 3.1
- Lombok
- Maven Wrapper

Required runtime: Java 25.

## Runtime Data

User data is stored locally in:

```text
backend/data/app_data.db
```

This runtime database is ignored by git. If the database does not exist, the backend initializes schema from:

```text
src/main/resources/schema.sql
```

The backend does not bundle dictionary data. Phrase source data and dictionary releases are maintained in the independent `LyricVocabularyDictionary` repository and supplied to a runtime separately.

Dictionary integration is disabled by default. Song, lyric, vocabulary-index, and personal-vocabulary APIs remain available without it; dictionary and dictionary-backed phrase results degrade to empty/not-found responses. Enable an external compatible database with `APP_DICTIONARY_ENABLED=true` and `APP_DICTIONARY_DB_URL=jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite`.

## Main Domains

```text
song/          songs and async import tasks
lyric/         normalization, lyric lines, tokens, line corrections
vocabulary/    lemma index, word occurrences, personal vocabulary, review queue
dictionary/    optional offline dictionary lookup
common/        response envelope, errors, shared infrastructure
config/        SQLite datasources and migrations
```

## API Contract

Canonical OpenAPI file:

```text
src/main/resources/api-docs.yaml
```

All successful JSON responses are wrapped as:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

The frontend generated client unwraps `data` at the transport layer.

## Important Endpoints

```text
GET    /api/songs
GET    /api/songs/count
POST   /api/songs
POST   /api/songs/import
GET    /api/songs/import/tasks/{taskId}

GET    /api/songs/{songId}/lyrics
POST   /api/songs/{songId}/lyrics/import
PUT    /api/songs/{songId}/lyrics/lines/{lineId}

GET    /api/vocabulary/words
GET    /api/vocabulary/words/{word}/occurrences
POST   /api/vocabulary/refresh

POST   /api/user-vocabulary
GET    /api/user-vocabulary
PATCH  /api/user-vocabulary/{id}
PATCH  /api/user-vocabulary/batch
DELETE /api/user-vocabulary/batch
POST   /api/user-vocabulary/import
GET    /api/user-vocabulary/stats
GET    /api/user-vocabulary/review

GET    /api/dictionary/{word}

GET    /api/backup/export
POST   /api/backup/validate
POST   /api/backup/restore
DELETE /api/data/all
```

## Local Development

```bash
./mvnw spring-boot:run
```

Default API base URL:

```text
http://localhost:8080
```

## Build And Test

```bash
./mvnw clean test
./mvnw clean package
```

Runtime configuration:

```text
APP_DB_URL=jdbc:sqlite:data/app_data.db
APP_DICTIONARY_ENABLED=false
APP_DICTIONARY_DB_URL=jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite
APP_CORS_ALLOWED_ORIGINS=http://localhost:9000,http://127.0.0.1:9000
```

The default operating boundary is localhost or a trusted LAN. This API has no user authentication or authorization. Do not expose it directly to the public internet; CORS is not an authentication mechanism.

## Copyright Boundary

This backend does not ship a lyric corpus or dictionary data. It stores only lyrics imported by the user and should be used with lyrics the user has the right to process. Externally supplied dictionary files remain the responsibility of the runtime operator.
