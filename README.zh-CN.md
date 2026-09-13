# Lyric Vocabulary Builder

[English](README.md) | [简体中文](README.zh-CN.md)

Lyric Vocabulary Builder 是一个面向中文母语者的英文歌词词汇学习应用。本应用支持用户将自己享有合法使用权的英文歌词结构化为可查询、可追踪、可复习的个人学习材料。

应用聚焦一个很明确的学习闭环：导入歌词，清洗并结构化歌词，在歌词语境中查询单词，查看英英释义和中文解释，再把词加入个人词库持续复习。

## 核心亮点

- **歌词结构化导入**：支持 TXT、JSON、LRC、SRT 和手动粘贴，围绕学习材料准备设计。
- **可恢复的清洗结果**：保留原始歌词、标准化歌词、行分类和用户修正，使更改可还原。
- **跨歌曲 lemma 搜索**：`running`、`ran`、`runs` 等词形可归并到 `run`。
- **歌词语境学习**：单词不是孤立背诵，而是在真实歌词行中出现。
- **可选外部词典整合**：词典由运行环境单独提供，主项目不随包携带任何词库，也支持无词库运行。
- **个人词库闭环**：支持加入单词、更新学习状态、记录熟悉度、查看统计和待复习词。
- **中文学习者友好**：应用始终围绕“中文用户用英文歌学英文”的定位，为中文学习者打造更容易接受的英文学习体验。

## 词典数据

词典数据由独立的 [LyricVocabularyDictionary](https://github.com/EachYoungX/LyricVocabularyDictionary) 项目维护和发布，不随本项目源码或应用安装包提供。

从 [LyricVocabularyDictionary 1.0.0 发布页](https://github.com/EachYoungX/LyricVocabularyDictionary/releases/tag/1.0.0) 下载并解压 `dictionary.sqlite`，然后在桌面应用的“设置 → 词典数据”中打开默认目录，将文件移入后点击“重新扫描”。也可以在同一位置选择其他目录中的兼容词典文件。词典文件移动或删除后，需要重新选择或扫描。

## 应用流程

当前学习流程保持线性：

1. 从 TXT、JSON、LRC、SRT 或手动粘贴导入歌词。
2. 标准化歌词文本，同时保留原始导入版本和学习文本版本。
3. 查看或编辑学习文本，不破坏首次导入的原文。
4. 对歌词行分词、归一词形，并生成基于 lemma 的词汇索引。
5. 在真实歌词语境中查询单词，并查看英英释义和中文解释。
6. 把值得学习的词加入个人词库，后续持续更新学习状态。

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3, Quasar 2, TypeScript, Pinia, Vue Router, Axios |
| 后端 | Java 25, Spring Boot 3.5, Spring Web, Spring Data JPA |
| 数据库 | SQLite 用户数据库 + 可选外部 SQLite 词典 |
| API 契约 | OpenAPI 3.1, generated TypeScript client |
| 工具链 | Maven Wrapper, pnpm, ESLint, Vite |

## 系统架构

系统可以简单理解为四层：

- 学习者通过 Vue 和 Quasar 前端导入歌曲、浏览词汇、管理歌词并维护个人词库状态。
- 前端通过 OpenAPI 生成的 TypeScript 客户端调用 Spring Boot API。
- 后端把歌曲、结构化歌词行、词汇 token 和个人学习记录保存在本地 SQLite 用户数据库中。
- 词汇索引构建器从用户导入的歌词中生成可搜索的 lemma 词条；词典查询仅在运行环境通过外部 SQLite 配置后启用，主项目不打包词典数据。

## 本地运行

环境要求：Java 25、Node.js 20 或更高版本、pnpm 11。仓库已经包含 Maven Wrapper，无需单独安装 Maven。

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

默认 API 地址：

```text
http://localhost:8080
```

开发配置使用本地 SQLite 用户数据库：

```text
backend/data/app_data.db
```

如果数据库不存在，后端会按 `schema.sql` 初始化，并执行必要的轻量迁移。

默认以无词库模式运行。需要使用外部词典时，在运行环境设置：

```text
APP_DICTIONARY_ENABLED=true
APP_DICTIONARY_DB_URL=jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite
```

无词典模式下，歌曲导入、歌词编辑、词汇索引和个人词库可正常使用；词典释义与依赖词典的短语匹配返回空结果或未找到，不会访问词典表。显式以无词典模式运行：

```bash
APP_DICTIONARY_ENABLED=false ./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Quasar 会在终端输出开发地址，通常为：

```text
http://localhost:9000
```

### 重新生成前端 API 客户端

后端 OpenAPI 契约位于：

```text
backend/src/main/resources/api-docs.yaml
```

生成客户端：

```powershell
cd frontend
pnpm gen-api
```

生成后脚本会自动适配后端 `{ code, message, data }` 响应信封。

开发环境默认连接 `http://localhost:8080`。API 位于其他主机时设置 `VITE_API_BASE_URL`；生产构建默认使用同源 `/api`，适合通过反向代理部署。局域网开发需要把前端的精确来源加入后端，例如 `APP_CORS_ALLOWED_ORIGINS=http://192.168.1.20:9000`，并让前端连接局域网可访问的后端地址。

## 验证

后端：

```bash
cd backend
./mvnw clean test
```

前端：

```powershell
cd frontend
pnpm lint
pnpm test
pnpm build
```

## 部署说明

当前项目适合本地学习工具、单机演示或小规模自托管部署。

1. 构建并运行后端：

```bash
cd backend
./mvnw clean package
java -jar target/backend-1.0.0.jar
```

2. 构建前端：

```powershell
cd frontend
pnpm build
```

3. 将 `frontend/dist/spa` 作为静态站点部署，并把 API 请求转发到后端。

默认部署范围是 localhost 或受信任局域网。后端没有用户认证和权限控制，CORS 也不是认证机制，因此不建议把后端 API 直接暴露到公网。需要扩大访问范围时，应由受信任的反向代理提供认证和 TLS。`backend/data/app_data.db` 不应进入仓库，不应公开用户导入的歌词或学习数据；词典文件由部署环境单独管理，不进入本项目发布物。

## 备份与恢复

数据管理页会导出带版本号的完整备份，包括歌曲源数据、结构化歌词与用户修正、创作人员、个人词库、用户短语、持久化词汇覆盖规则、界面语言、动效设置和已保留的清洗候选决策。恢复会在修改数据前验证完整备份，并通过事务执行覆盖恢复；恢复完成后重新生成派生词汇与短语缓存。个人词库 CSV 是只包含 `lemma`、`status`、`note` 的轻量迁移格式；需要精确往返时间戳和学习状态时使用完整 JSON 备份。

## 数据与版权边界

- 本仓库不包含、不分发歌词库。
- 用户仅应导入自己拥有使用权或有权处理的歌词文本。
- 应用完全本地，不会上传任何本地数据，也不会读取未授权的外部文件。
- 应用会把用户导入的歌词、清洗结果、词汇索引和个人学习状态保存到本地数据库。
- 词典源数据和发布包由独立的 [LyricVocabularyDictionary](https://github.com/EachYoungX/LyricVocabularyDictionary) 项目维护；本项目不包含、不分发词典文件。用户自行配置外部词典时，应自行核实其来源、许可证和使用权限。
- 本项目不对外部词典数据的准确性、完整性、授权状态或适用性作任何保证。

## 项目结构

```text
.
├── backend/                 # Spring Boot API
│   ├── src/main/java/       # domain code
│   ├── src/main/resources/  # schema, OpenAPI, runtime configuration（不含词典）
│   └── data/                # local runtime database, ignored by git
├── frontend/                # Quasar Vue app
│   ├── src/components/
│   ├── src/pages/
│   ├── src/services/api/    # generated OpenAPI client
│   └── src/css/             # visual tokens and global styles
├── assets/screenshots/      # README 截图
├── CHANGELOG.md             # 已完成并验证的阶段记录
└── README.md                # 默认英文 README
```

2ndLA 原始词库、初步整理 JSON、翻译批次和短语发布版 SQLite 均不在本仓库维护。词典项目与本项目独立发布；本项目只通过 `APP_DICTIONARY_DB_URL` 接入运行环境提供的外部 SQLite 文件。

## 当前状态

开发阶段 Stage 0 到 Stage 7 已完成并通过验证。详情见 [CHANGELOG.md](CHANGELOG.md)。
