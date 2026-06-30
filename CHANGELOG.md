# 变更日志 / Changelog

本文件只记录已经完成并通过验证的重构阶段。内部计划和审计资料不纳入仓库。

This file records only completed and verified refactoring stages. Internal plans and audit notes are excluded from the repository.

## Stage 4 - 2026-06-25

### 稳定性、索引性能与代码可读性 / Stability, Index Performance, and Code Quality

- 将词汇索引构建逻辑从 `VocabularyServiceImpl` 拆分到独立的 `VocabularyIndexBuilder`，让任务状态、分页查询和索引构建职责分离。
- 词汇重建从逐首歌曲查询歌词行改为一次批量加载结构化歌词行，降低重建阶段的 N+1 查询风险。
- 结构化 token 仍批量保存，并保持无结构化歌词行时的旧歌词文本 fallback。
- 为歌词行、token lemma、推荐词列表、用户词汇状态和词汇出现位置补齐 JPA 与 SQLite schema 索引声明。
- 为离线词典查询增加有上限的本地缓存，避免重复查询同一词条反复访问 SQLite。
- 关闭开发和测试配置中的 SQL 明细输出，并关闭测试环境 `open-in-view`，减少噪音和隐藏查询风险。
- 新增索引构建器与词典缓存测试，覆盖批量歌词行加载、lemma 聚合、fallback 和重复词典查询。

### Verification

- Backend: 57 tests passed with 0 failures and 0 errors.
- Frontend: ESLint, TypeScript checks, and Quasar production build passed.
- Existing SQLite database migration for the additional indexes passed.
- Real API regression passed: index rebuild completed, `run` and `running` returned the same 8 occurrences, low-value `yeah` stayed out of the default word list, and dictionary source metadata remained available.

## Stage 3 - 2026-06-25

### 词形归一、词汇索引与词典元数据 / Lemma-Based Vocabulary Index and Dictionary Metadata

- 新增 `lyric_tokens` 结构化 token 模型，记录歌词行、原始词形、标准化词形、lemma、文本偏移、token 类型和学习价值评分。
- 词汇索引重建改为基于结构化歌词行生成 token，再按 lemma 聚合出现位置；搜索 `running`、`ran`、`runs` 可归并到 `run`。
- 默认词汇列表只展示推荐学习词；低价值词和常见歌词填充词仍可保留为 token，但不会污染默认学习统计。
- 扩展词汇出现位置返回歌曲 id、歌词行 id、行号、surface form、lemma、offset 和学习评分，保留旧字段兼容前端页面。
- 新增 `user_vocabulary`、`vocabulary_occurrences` 等用户学习状态表，和开源词典/歌词 token 索引分离。
- 新增词典来源与授权信息接口，并在前端添加双语“词典来源与版权说明”页面。
- 后端 OpenAPI 升级到 `1.3.0`，前端 API 客户端从后端契约重新生成。

### Verification

- Backend: 54 tests passed with 0 failures and 0 errors.
- Frontend: ESLint, TypeScript checks, and Quasar production build passed.
- Canonical OpenAPI client regeneration passed.
- Existing SQLite database migration for token, user vocabulary, and occurrence tables passed.
- Real API regression passed: index rebuild completed, `run` and `running` returned the same 8 occurrences, low-value `yeah` stayed out of the default word list, and dictionary source metadata returned ECDICT / MIT License.

## Stage 2 - 2026-06-22

### 歌词导入、清洗与结构化 / Lyric Import, Normalization, and Structuring

- 在保留旧接口兼容的同时，新增原始歌词、标准化歌词、SHA-256 hash、导入版本和更新时间。
- 新增结构化歌词行模型，支持歌词、段落标签、角色标签、演奏说明、元信息、空行和未识别类型。
- 标准化规则支持统一换行、LRC 时间戳移除、全角字符归一、空白清理和重复空行合并。
- 非歌词内容采用默认隐藏而非不可逆删除，并记录分类置信度。
- 新增逐行用户修正 API；覆盖导入时，匹配原始文本的用户 override 优先保留。
- 相同歌词重复导入保持 hash、版本和结构行不变；不同歌词在未明确允许覆盖时返回 `409`。
- 既有 SQLite 数据库采用增量迁移，并在启动时自动补建结构化歌词行。
- 词汇索引改为读取标准化歌词，同时保留原始歌词用于恢复和对照。
- 歌曲管理页新增双语结构化歌词对话框，可查看原文/标准化结果、分类、隐藏状态、置信度并逐行修正。

### Verification

- Backend: 49 tests passed with 0 failures and 0 errors.
- Frontend: ESLint, TypeScript checks, and Quasar production build passed.
- Canonical OpenAPI client regeneration passed.
- Existing 6-song SQLite database migration and lyric-line backfill passed.
- Same-content idempotency, different-content `409`, overwrite versioning, and override preservation passed real API regression.
- Structured lyric UI and portrait/landscape responsive layouts passed browser regression.

## Stage 1 - 2026-06-21

### 结构重构与职责拆分 / Structural Refactoring

- 后端按 `song`、`dictionary`、`vocabulary` 和 `common` 领域重组核心代码。
- 统一成功与错误响应为 `{ code, message, data }`，并增加稳定的业务异常码。
- 保留现有 API URL，前端传输层自动解包业务数据，页面调用方式保持兼容。
- 后端 OpenAPI 成为唯一接口契约来源，前端生成流程包含可复现的 envelope 后处理。
- 删除重复且未使用的导入任务服务、旧错误 DTO 和旧前端 API 包装层。
- 抽离词汇浏览和索引重建 composables，清理 Pinia store 的重复领域状态。
- 将中英文切换移到全局顶栏，使词汇页与歌曲页均可切换语言。

### Verification

- Backend: 35 tests passed.
- Frontend: ESLint, TypeScript checks, and Quasar production build passed.
- OpenAPI client regeneration passed from the canonical backend specification.
- Vocabulary lookup, lyric context, bilingual definitions, song management, and responsive portrait/landscape layouts passed manual regression.

## Stage 0 - 2026-06-21

### 现状审计与冻结 / Current-State Audit and Freeze

- 完成前后端模块、接口、数据模型和页面调用链审计。
- 建立自动化与人工回归基线。
- 修复测试环境词典数据源配置和已失真的词典服务测试。
- 修复 Windows PowerShell 5 下 Maven Wrapper 的启动兼容问题。
- 验证后端 39 个测试、前端 lint 和生产构建全部通过。
- 验证词汇查询、歌词语境、双语词典、歌曲列表及手机横竖屏基线。

### Baseline Notes

- No production business behavior was changed.
- Structured lyrics, lemmatization, and personal vocabulary remain future work.
