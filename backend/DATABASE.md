# Lyric Vocabulary Builder - 数据库设计说明文档

## 1. 概述

本项目的后端数据存储采用 **SQLite** 数据库，其设计哲学旨在平衡性能、开发效率与数据一致性。整个应用涉及两个独立的数据库文件，分别承担不同的职责：

1.  **`app_data.sqlite`**: **用户数据库**。存储所有用户导入的、动态增长的数据。这是应用的核心读写数据库。
2.  **`dictionary.sqlite`**: **词典数据库**。一个经过优化的、静态的、只读的资源数据库，作为内置的英英词典。

本文档将分别对这两个数据库进行详细说明。

---

## 2. 用户数据库 (`app_data.sqlite`)

### 2.1. 文件位置

- **开发环境**: `app_data.sqlite` 文件位于项目根目录下的 `data/` 文件夹中，便于开发和调试。
- **生产环境 (打包后)**: 为了保证用户数据的持久性和安全性，数据库文件会自动创建并存储在用户的主目录下的特定应用文件夹中。
  - **路径**: `${user.home}/.lyricbuilder/app_data.sqlite`
  - **自动管理**: 应用程序启动时会自动检查并创建此目录和数据库文件。

### 2.2. 设计原则

- **查询性能优先**: 针对应用高频的“单词查询”场景，采用了适度的反范式设计，通过空间换时间，避免复杂的 `JOIN` 操作。
- **职责分离**: 原始数据（歌曲）和衍生数据（词汇索引）存储在不同的表中，逻辑清晰。
- **简单可靠**: 采用“完全重建索引”的策略来处理数据更新，避免了维护复杂数据一致性的难题，使代码更简单、更健壮。

### 2.3. 表结构详解

#### 2.3.1. `songs` 表

**用途**: 存储用户导入的所有歌曲的原始文本信息。这是所有数据分析的源头。

**SQL 定义**:
```sql
CREATE TABLE songs (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    title  TEXT NOT NULL,
    artist TEXT NOT NULL,
    lyrics TEXT NOT NULL,
    UNIQUE(title, artist)
);
```

**字段说明**:

| 字段名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY` | 歌曲的唯一标识符，自增。 |
| `title` | `TEXT` | `NOT NULL` | 歌曲的标题。 |
| `artist` | `TEXT` | `NOT NULL` | 歌曲的艺术家/歌手。 |
| `lyrics` | `TEXT` | `NOT NULL` | 完整的歌曲正文，包含换行符。 |
| `(title, artist)` | - | `UNIQUE` | 联合唯一约束，确保不会重复添加“同名同歌手”的歌曲。 |

**JPA 实体类**: `com.yourname.lyricbuilder.model.Song`

#### 2.3.2. `vocabulary` 表

**用途**: 存储从所有歌曲中提取出的词汇“倒排索引”。这是一个为了极致查询性能而设计的“物化视图”或“缓存表”。

**SQL 定义**:
```sql
CREATE TABLE vocabulary (
    word        TEXT PRIMARY KEY,
    occurrences TEXT NOT NULL
);
```

**字段说明**:

| 字段名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `word` | `TEXT` | `PRIMARY KEY` | 经过清洗和标准化的小写英文单词。作为主键，保证每个单词只有一条索引记录。 |
| `occurrences` | `TEXT` | `NOT NULL` | **JSON 字符串**。存储一个 JSON 数组，记录该单词在所有歌曲中每一次出现的位置。 |

**`occurrences` 字段 JSON 结构示例**:

```json
[
  {
    "songTitle": "Let It Be",
    "lyricLine": "When I find myself in times of trouble, Mother Mary comes to me"
  }
]
```

**JPA 实体类**: `com.yourname.lyricbuilder.model.Vocabulary`

### 2.4. 核心数据流与刷新机制

1.  **数据写入**: 当用户通过 `POST /api/songs/import` 接口导入新歌时，歌曲数据仅写入 **`songs`** 表。
2.  **索引生成与刷新**: `vocabulary` 表的数据**不是**在歌曲导入时实时更新的。当接收到 `POST /api/vocabulary/refresh` 请求时，后端会启动一个异步任务，该任务会清空 `vocabulary` 表，然后遍历 `songs` 表中的所有歌曲，重新计算索引并批量写入。
3.  **数据查询**: 当用户通过 `GET /api/vocabulary/words/{word}/occurrences` 查询单词时，后端**仅**查询 `vocabulary` 表，直接返回 `occurrences` 字段的 JSON 内容，速度极快。

---

## 3. 词典数据库 (`dictionary.sqlite`)

### 3.1. 作用与性质

- **作用**: 提供一个内置的、离线的英英词典，用于查询单词的音标、释义、词形变化等详细信息。
- **性质**: **静态资源，只读**。该数据库是应用程序的一部分，随程序打包分发，**运行时不会被任何业务逻辑修改**。

### 3.2. 数据预处理与优化 (重要)

此数据库文件是基于 [ECDICT 开源项目](https://github.com/skywind3000/ECDICT) 的原始 `ecdict.sqlite` 文件经过**严格的预处理**生成的。预处理流程（详见 `scripts/prepare_dictionary.py`）旨在实现以下目标：

1.  **精简数据**: 移除了原始数据库中与本项目核心功能无关的大量字段（如 `sw`, `oxford`, `tag`, `detail`, `audio` 等）。
2.  **优化结构**: 对保留的字段进行了重命名，使其更具可读性（如 `exchange` -> `forms`）。
3.  **清理数据**: 过滤了少量无效或格式错误的词条。

**核心成果**: 通过此优化流程，词典数据库的体积**大幅缩减至约 392MB**，在不影响核心查询功能的前提下，显著减小了最终软件包的体积，并提升了查询性能。

### 3.3. 文件位置

- **开发环境**: 位于 Spring Boot 项目的 `src/main/resources/` 目录下。
- **生产环境 (打包后)**: 文件被打包进最终的可执行 `.jar` 文件内部。
- **访问方式**: 应用程序通过 **Classpath 资源** (`jdbc:sqlite::resource:dictionary.sqlite`) 来访问它，确保了无论 `.jar` 文件在哪里运行，都能正确找到该数据库。

### 3.4. 表结构详解

数据库中仅包含一张表：`dictionary`。

**`dictionary` 表 SQL 定义**:
```sql
CREATE TABLE dictionary (
    word         TEXT PRIMARY KEY,
    phonetic     TEXT,
    definition   TEXT,
    translation  TEXT,
    pos          TEXT,
    collins_star INTEGER,
    bnc_rank     INTEGER,
    frq_rank     INTEGER,
    forms        TEXT
);
```

**字段说明**:

| 字段名 | 类型 | 描述 | 示例内容 (以单词 "voyage" 为例) |
| :--- | :--- | :--- | :--- |
| `word` | `TEXT` | **主键**。经过清洗的小写英文单词。 | `voyage` |
| `phonetic` | `TEXT` | 音标。 | `/ˈvɔɪ.ɪdʒ/` |
| `definition` | `TEXT` | **核心字段**：英文释义。 | `a long journey, especially by ship or in space` |
| `translation`| `TEXT` | 中文翻译。 | `航行；旅行` |
| `pos` | `TEXT` | 词性缩写 (part of speech)。 | `noun, verb` |
| `collins_star`| `INTEGER`| 柯林斯星级 (1-5)。 | `5` |
| `bnc_rank` | `INTEGER`| BNC (英国国家语料库) 词频排名。 | `3025` |
| `frq_rank` | `INTEGER`| COCA (美国当代英语语料库) 词频排名。 | `2632` |
| `forms` | `TEXT` | 词形变化。格式为 `类型:值`，用 `/` 分隔。 | `p:voyaged/d:voyaged/i:voyaging/s:voyages` |

**`forms` 字段类型说明**:
- `p`: 过去式 (past tense)
- `d`: 过去分词 (past participle)
- `i`: 现在分词 (ing)
- `3`: 第三人称单数 (3rd person singular)
- `r`: 比较级 (comparative)
- `t`: 最高级 (superlative)
- `s`: 复数 (plural)