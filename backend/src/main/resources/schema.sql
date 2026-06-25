-- 创建 songs 表，并在此处明确定义 UNIQUE 约束
CREATE TABLE IF NOT EXISTS songs (
                                     id     INTEGER PRIMARY KEY AUTOINCREMENT,
                                     title  TEXT NOT NULL,
                                     artist TEXT NOT NULL,
                                     lyrics TEXT NOT NULL,
                                     raw_lyrics TEXT,
                                     normalized_lyrics TEXT,
                                     lyrics_hash TEXT,
                                     import_version INTEGER NOT NULL DEFAULT 1,
                                     updated_at TEXT,
                                     UNIQUE(title, artist)
);

CREATE TABLE IF NOT EXISTS lyric_lines (
                                           id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                           song_id         INTEGER NOT NULL,
                                           line_index      INTEGER NOT NULL,
                                           original_text   TEXT NOT NULL,
                                           normalized_text TEXT NOT NULL,
                                           line_type       TEXT NOT NULL,
                                           hidden          INTEGER NOT NULL DEFAULT 0,
                                           confidence      REAL NOT NULL,
                                           user_override   INTEGER NOT NULL DEFAULT 0,
                                           UNIQUE(song_id, line_index),
                                           FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lyric_lines_song
    ON lyric_lines(song_id, line_index);

CREATE TABLE IF NOT EXISTS lyric_tokens (
                                            id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                            lyric_line_id   INTEGER NOT NULL,
                                            surface_form    TEXT NOT NULL,
                                            normalized_form TEXT NOT NULL,
                                            lemma           TEXT NOT NULL,
                                            start_offset    INTEGER NOT NULL,
                                            end_offset      INTEGER NOT NULL,
                                            token_type      TEXT NOT NULL,
                                            learning_score  REAL NOT NULL,
                                            FOREIGN KEY(lyric_line_id) REFERENCES lyric_lines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lyric_tokens_line
    ON lyric_tokens(lyric_line_id);

CREATE INDEX IF NOT EXISTS idx_lyric_tokens_lemma
    ON lyric_tokens(lemma);

-- 创建 vocabulary 表
CREATE TABLE IF NOT EXISTS vocabulary (
                                          word             TEXT PRIMARY KEY,
                                          occurrences      TEXT NOT NULL,
                                          display_forms    TEXT,
                                          occurrence_count INTEGER NOT NULL DEFAULT 0,
                                          song_count       INTEGER NOT NULL DEFAULT 0,
                                          learning_score   REAL NOT NULL DEFAULT 1.0,
                                          recommended      INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS user_vocabulary (
                                               id            INTEGER PRIMARY KEY AUTOINCREMENT,
                                               user_id       TEXT NOT NULL DEFAULT 'local',
                                               lemma         TEXT NOT NULL,
                                               status        TEXT NOT NULL DEFAULT 'NEW',
                                               mastery_score REAL NOT NULL DEFAULT 0,
                                               first_seen_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               last_seen_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               review_due_at TEXT,
                                               note          TEXT,
                                               UNIQUE(user_id, lemma)
);

CREATE INDEX IF NOT EXISTS idx_user_vocabulary_user_status
    ON user_vocabulary(user_id, status);

CREATE TABLE IF NOT EXISTS vocabulary_occurrences (
                                                     id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                                                     user_vocabulary_id INTEGER,
                                                     song_id            INTEGER NOT NULL,
                                                     lyric_line_id      INTEGER NOT NULL,
                                                     token_id           INTEGER NOT NULL,
                                                     UNIQUE(user_vocabulary_id, token_id),
                                                     FOREIGN KEY(user_vocabulary_id) REFERENCES user_vocabulary(id) ON DELETE CASCADE,
                                                     FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE,
                                                     FOREIGN KEY(lyric_line_id) REFERENCES lyric_lines(id) ON DELETE CASCADE,
                                                     FOREIGN KEY(token_id) REFERENCES lyric_tokens(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vocabulary_occurrences_song
    ON vocabulary_occurrences(song_id);

CREATE INDEX IF NOT EXISTS idx_vocabulary_occurrences_user_vocab
    ON vocabulary_occurrences(user_vocabulary_id);
