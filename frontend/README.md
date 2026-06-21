# Lyric Vocabulary Builder 前端应用

一个基于 Quasar Framework + Vue 3 + TypeScript 的前端应用，提供现代化的用户界面来管理英文歌曲库、构建词汇索引并进行英英词典查询。

## 技术栈

- **框架**: Quasar Framework 2.16.0
- **语言**: TypeScript 5.9.2 + Vue 3.5.22
- **状态管理**: Pinia 3.0.1
- **路由**: Vue Router 4.0.12
- **HTTP客户端**: Axios 1.2.1
- **UI组件**: Quasar UI 组件库
- **构建工具**: Vite 5.x
- **包管理**: pnpm
- **代码规范**: ESLint + Prettier

## 项目结构

```
frontend/
├── src/
│   ├── components/           # 可复用组件
│   │   ├── SongImportDialog.vue  # 歌曲导入对话框
│   │   └── models.ts         # 类型定义
│   ├── layouts/              # 布局组件
│   │   └── MainLayout.vue    # 主布局
│   ├── pages/                # 页面组件
│   │   ├── IndexPage.vue     # 主页
│   │   └── ErrorNotFound.vue # 404页面
│   ├── services/             # API服务层
│   │   ├── api/              # OpenAPI生成的服务
│   │   │   ├── core/         # 核心API工具
│   │   │   ├── models/       # 数据模型
│   │   │   └── services/     # API服务
│   │   ├── DictionaryService.ts    # 词典服务
│   │   └── ExtendedSongsService.ts # 扩展歌曲服务
│   ├── stores/               # Pinia状态管理
│   │   ├── vocabularyStore.ts     # 词汇状态管理
│   │   ├── dictionaryStore.ts     # 词典状态管理
│   │   └── songsStore.ts          # 歌曲状态管理
│   ├── router/               # 路由配置
│   │   ├── index.ts          # 路由入口
│   │   └── routes.ts         # 路由定义
│   ├── boot/                 # 启动配置
│   │   ├── axios.ts          # Axios配置
│   │   └── i18n.ts           # 国际化配置
│   ├── css/                  # 样式文件
│   │   ├── app.scss          # 应用样式
│   │   └── quasar.variables.scss # Quasar变量
│   └── App.vue               # 应用根组件
├── api-docs.yaml             # OpenAPI规范
├── frontend-spec.md          # 前端详细规范
├── package.json              # 项目配置
└── eslint.config.js          # ESLint配置
```

## 核心功能

### 1. 词汇管理
- **单词列表展示**: 分页显示所有唯一单词
- **搜索过滤**: 支持前缀搜索和实时过滤
- **单词详情**: 点击单词查看详细信息

### 2. 歌曲管理
- **批量导入**: 异步导入多首歌曲文本
- **导入状态跟踪**: 实时监控导入任务进度
- **词汇索引重建**: 一键重建词汇索引

### 3. 词典查询
- **英英词典**: 查询单词的音标、词性、释义
- **实时查询**: 选择单词自动查询词典
- **错误处理**: 友好的错误提示和空状态

### 4. 用户界面
- **响应式布局**: 适配不同屏幕尺寸
- **分栏设计**: 左侧单词列表，右侧详情展示
- **水平分割**: 上部显示出现位置，下部显示词典定义

## 状态管理设计

### vocabularyStore (词汇状态管理)
```typescript
interface VocabularyState {
  words: string[]           // 单词列表
  selectedWord: string     // 当前选中单词
  wordOccurrences: WordOccurrence[] // 单词出现位置
  currentPage: number      // 当前页码
  pageSize: number         // 每页大小
  totalPages: number       // 总页数
  isLoading: boolean       // 加载状态
}
```

### dictionaryStore (词典状态管理)
```typescript
interface DictionaryState {
  dictionaryEntry: DictionaryEntry | null // 词典条目
  isLoading: boolean       // 加载状态
  error: string | null    // 错误信息
}
```

### songsStore (歌曲状态管理)
```typescript
interface SongsState {
  importTask: ImportTaskResult | null // 导入任务
  isImporting: boolean     // 导入状态
  importProgress: number   // 导入进度
}
```

## API服务层

### 自动生成的服务
基于 `api-docs.yaml` 通过 OpenAPI 自动生成：
- **VocabularyService**: 词汇相关API
- **SongsService**: 歌曲管理API
- **DictionaryService**: 词典查询API

### 扩展服务
- **ExtendedSongsService**: 增强的歌曲服务，支持任务状态轮询
- **DictionaryService**: 包装的词典服务，提供更好的错误处理

## 组件设计

### MainLayout.vue (主布局)
- 顶部工具栏：应用标题、导入按钮、刷新按钮
- 左侧抽屉：导航菜单
- 主内容区：路由视图容器
- 模态对话框：歌曲导入对话框

### IndexPage.vue (主页)
- **QSplitter分栏**: 30%单词列表 + 70%详情区域
- **水平分割**: 50%出现位置 + 50%词典定义
- **搜索框**: 实时过滤单词列表
- **分页控件**: 支持大列表浏览

### SongImportDialog.vue (导入对话框)
- **多行文本输入**: 支持批量粘贴歌曲文本
- **异步导入**: 非阻塞式导入处理
- **进度显示**: 实时显示导入状态
- **错误处理**: 友好的错误提示

## 配置说明

### package.json 关键配置
```json
{
  "scripts": {
    "dev": "quasar dev",           // 开发服务器
    "build": "quasar build",       // 生产构建
    "lint": "eslint src/**/*.{ts,js,vue}", // 代码检查
    "gen-api": "openapi-typescript-codegen" // API代码生成
  }
}
```

### ESLint配置 (eslint.config.js)
- TypeScript严格模式
- Vue 3推荐规则
- Prettier集成
- 自动修复支持

### Quasar配置
- Material Design风格
- 响应式断点配置
- 国际化支持
- 图标库配置

## 开发环境

### 系统要求
- Node.js 20+
- pnpm 8+
- 现代浏览器 (Chrome 90+, Firefox 88+, Safari 14+)

### 安装依赖
```bash
pnpm install
```

### 开发模式运行
```bash
pnpm dev
```

### 生产构建
```bash
pnpm build
```

### 代码检查
```bash
pnpm lint
```

### 代码格式化
```bash
pnpm format
```

### API代码生成
```bash
pnpm gen-api
```

## 部署说明

### 构建产物
构建后的文件位于 `dist/` 目录：
- `index.html`: 入口文件
- `assets/`: 静态资源
- `js/`: JavaScript文件
- `css/`: 样式文件

### 部署方式
1. **静态文件服务器**: 直接部署到Nginx、Apache等
2. **CDN部署**: 上传到云存储服务
3. **Docker部署**: 使用Nginx容器

### 环境变量
- `VITE_API_BASE_URL`: API基础URL
- `VITE_APP_TITLE`: 应用标题

## 测试

### 运行测试
```bash
pnpm test
```

### 组件测试
项目支持Vue Test Utils进行组件测试

## 许可证

MIT License