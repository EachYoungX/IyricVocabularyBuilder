# 变更日志 / Changelog

本文件只记录已经完成并通过验证的重构阶段。内部计划和审计资料不纳入仓库。

This file records only completed and verified refactoring stages. Internal plans and audit notes are excluded from the repository.

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
