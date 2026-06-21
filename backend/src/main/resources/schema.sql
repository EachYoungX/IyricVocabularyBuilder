-- 创建 songs 表，并在此处明确定义 UNIQUE 约束
CREATE TABLE IF NOT EXISTS songs (
                                     id     INTEGER PRIMARY KEY AUTOINCREMENT,
                                     title  TEXT NOT NULL,
                                     artist TEXT NOT NULL,
                                     lyrics TEXT NOT NULL,
                                     UNIQUE(title, artist)
);

-- 创建 vocabulary 表
CREATE TABLE IF NOT EXISTS vocabulary (
                                          word        TEXT PRIMARY KEY,
                                          occurrences TEXT NOT NULL
);