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

-- 创建 vocabulary 表
CREATE TABLE IF NOT EXISTS vocabulary (
                                          word        TEXT PRIMARY KEY,
                                          occurrences TEXT NOT NULL
);
