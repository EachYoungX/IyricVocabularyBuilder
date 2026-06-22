# 变更日志 / Changelog

本文件只记录已经完成并通过验证的重构阶段。内部计划和审计资料不纳入仓库。

This file records only completed and verified refactoring stages. Internal plans and audit notes are excluded from the repository.

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
