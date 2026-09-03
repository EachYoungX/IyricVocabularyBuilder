# Lyric Vocabulary Builder - 数据库设计说明

## 1. 数据库边界

应用运行时可能访问两个职责独立的 SQLite 数据库：

1. `app_data.db`：用户数据库，保存歌曲、歌词、词汇索引和个人学习数据。
2. 外部词典 SQLite：可选的只读查询数据源，由独立词库项目或部署环境提供。

词典文件不属于本项目发布物，不放入源码仓库、JAR、前端静态资源或项目 Release。

## 2. 用户数据库 (`app_data.db`)

用户数据库由 `schema.sql` 初始化，并由 `DataSourceConfig` 执行必要的轻量迁移。主要数据包括：

- `songs`：用户导入的歌曲元信息及原始、标准化歌词。
- `lyric_lines`：结构化歌词行、行分类和用户修正。
- `lyric_tokens`：歌词中的词汇 token、词形和位置。
- `vocabulary`：从用户歌词生成的 lemma 索引。
- `vocabulary_occurrences`：词条在歌曲歌词中的出现位置。
- `user_vocabulary`：用户加入个人词库的单词和学习状态。
- `user_phrase`：用户保存的短语和学习状态。

用户导入的歌词和学习数据属于运行环境中的本地数据，不随本项目源码提交或发布。

## 3. 外部词典数据库

开发环境默认指向工作区外部的词典数据库。词典文件仍由运行环境单独提供，不进入本项目源码、JAR 或 Release。需要更换路径时，由运行环境配置：

```text
APP_DICTIONARY_ENABLED=true
APP_DICTIONARY_DB_URL=jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite
```

外部词典数据源使用只读 SQLite 数据源。本项目只依赖运行时约定的查询表结构，不负责词典源数据的构建、发布、许可证声明或再分发。

外部词典的来源、许可证、署名、内容质量和使用权限由提供该文件的独立词库项目及运行环境负责人负责。音乐项目不对这些内容作任何保证。
