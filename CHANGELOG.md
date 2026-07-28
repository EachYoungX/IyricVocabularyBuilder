# 变更日志 / Changelog

本文件只记录已经完成并通过验证的重构阶段。内部计划和审计资料不纳入仓库。

This file records only completed and verified refactoring stages. Internal plans and audit notes are excluded from the repository.

## Stage 7 - 2026-07-01

### 发布前整理与作品集化 / Release Readiness and Portfolio Packaging

- 后续修订：新增“我的词库”管理页，并增加词库清洗候选只读预览；用户可集中筛选个人词库、批量更新学习状态、批量移出个人词条，并查看低学习价值或疑似异常词的候选原因与歌词语境；个人词库页承接 CSV/Anki/JSON 导出、CSV/TSV 导入和清空学习记录；设置页移除日常词库管理入口，仅保留完整备份、恢复和全局清理；设置页补充低学习价值词说明，并将重复歌词统计收敛为“全部统计 / 相同行去重”两个实际生效选项；学习状态入口收敛为新词、学习中、已掌握和忽略，旧的较熟悉/仅收藏状态仅保留兼容显示。
- 将 README 拆分为默认英文版和独立中文版，并在开头提供语言切换入口；补齐项目定位、核心卖点、技术栈、系统架构、学习流程、本地运行、验证命令、部署说明和数据边界。
- 新增公开截图资源，展示词汇学习首页和歌曲管理页的当前视觉效果。
- 更新前端 README，说明页面结构、主题色卡、生成式 API 客户端、本地开发和构建方式。
- 更新后端 README，说明 Spring Boot 服务、SQLite 数据、结构化歌词、词汇索引、个人词库、OpenAPI 契约和词典授权来源。
- 明确歌词版权边界：仓库不内置第三方歌词，用户仅导入自己有权使用的文本；仓库内置词典来源为 ECDICT / MIT License。
- 使用真实歌词样本完成导入流程审计，覆盖单首导入、多首导入、TXT、LRC、加密 QRC、英文为主、夹杂中文/日文、纯中文和纯日文边界。
- 修正 QQ 音乐式 LRC 文件名的歌手/标题推断；加密 QRC 改为明确提示不支持直接解析，并引导用户使用 TXT/LRC/SRT 或手动粘贴。
- 修正非英语歌词预览文案参数缺失，避免显示“检测到 个文件”。
- 修正后端 CORS 漏放 `PATCH` 导致浏览器无法更新个人词汇学习状态的问题。
- 根据真实交互反馈优化导入弹窗、分页、详情区域视觉分隔、状态操作区、导航间距和通知位置。
- 新增多套完整主题 token 与深色主题 token，为后续设置页面管理主题预留基础。
- 简化 README Mermaid 图语法，避免 GitHub 无法渲染 rich display。
- 新增统一页面与元素浮现动效，并预留 `setMotionPreference` / `toggleMotionPreference` 一键禁用入口；默认遵循系统 reduced motion 设置。
- 新增设置页面入口，迁移词典来源展示，并以低耦合方式预留外观阅读、学习偏好、歌词处理、词典词库、数据管理和隐私说明框架。
- 设置页新增个人词库 CSV 导出、学习记录 JSON 导出、完整备份 JSON 导出，以及备份文件预览和设置偏好导入能力。
- 设置页清理操作接入真实行为：清理本地缓存、删除全部歌曲、删除全部学习记录，并新增清空个人词库后端接口。
- 设置偏好继续接入业务流程：新增“仅收藏”个人词汇状态；新词默认状态、歌词导入清洗策略、直接保存导入、词典展示内容、释义语言、词形归一搜索、短语优先识别、低学习价值词列表策略和低学习价值提示开始影响实际页面行为；个人词库新增 CSV/TSV 导入和 Anki TSV 导出。

### Verification

- Backend: 70 tests passed with 0 failures and 0 errors.
- Frontend: ESLint and Quasar production build passed.
- README screenshot links and public documentation paths verified.
- Browser flow passed for manual paste import, multi-song import, index rebuild, word lookup, dictionary display, adding a word to personal vocabulary, and updating vocabulary status.
- Browser layout audit passed for desktop and mobile import dialog overflow, mobile pagination, and vocabulary detail alignment.

## Stage 6 - 2026-07-01

### 页面美化与跨端体验 / Visual System and Responsive Experience

- 以单一完整色卡 `Midnight Sail`（`#1B3C53`、`#456882`、`#D2C1B6`、`#F9F3EF`）建立主视觉，保留极简白底与衬线标题气质。
- 新增全局设计 token，统一颜色、字体、圆角、边线、阴影、按钮、卡片、列表、表格、输入框、分页和 banner 的基础样式。
- 重做主布局视觉：顶部栏、侧边栏、品牌文字徽标、导航 active 状态和页面 masthead 形成一致的学习工具主视觉。
- 首页和歌曲管理页增加桌面、手机竖屏、手机横屏的响应式策略，减少横向溢出与移动端挤压。
- 导入弹窗与结构化歌词弹窗接入主题样式，增强歌词预览、行编辑和移动端换行体验。
- 清理重复的全局布局 CSS，将页面基础布局、安全区、触控尺寸和 dialog 规则集中到全局样式中维护。

### Verification

- Frontend: ESLint and Quasar production build passed.
- Browser responsive smoke test passed for desktop, mobile portrait, and mobile landscape on the vocabulary and songs pages.
- No backend code changed in this stage.

## Stage 5 - 2026-06-30

### 导入增强、个人词库与复习闭环 / Import Enhancements, Personal Vocabulary, and Review Loop

- 歌曲导入支持 JSON、TXT、LRC 和 SRT；LRC/SRT 会去除时间轴与格式标记，并保留更干净的歌词文本用于学习。
- 导入预览新增双语结构摘要，显示总行数、识别出的歌词行、段落标签、角色标签、演奏说明、元信息、默认隐藏行和未识别行。
- 新增个人词库 API，支持加入单词、按状态查询、更新学习状态、记录掌握度、统计学习数量和获取待复习队列。
- 首页新增个人词汇统计和待复习入口；词典面板可将当前词加入个人词库，并更新为新词、学习中、较熟悉、已掌握或忽略。
- 后端 OpenAPI 增加个人词库契约，前端 API 客户端重新生成；生成后处理兼容浏览器原生 `FormData`。
- 保留已有逐行歌词修正数据能力，和本阶段新增个人词汇学习数据共同形成可追踪的学习材料。

### Verification

- Backend: 64 tests passed with 0 failures and 0 errors.
- Frontend: ESLint and Quasar production build passed.
- Canonical OpenAPI client regeneration passed from the backend specification.

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
