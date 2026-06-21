# Lyric Vocabulary Builder - 前端实现说明文档

## 1. 项目概述与技术栈

### 1.1. 项目目标

本项目旨在为 "Lyric Vocabulary Builder" 后端 API 构建一个功能完整、界面现代、跨平台（桌面、移动、Web）的前端应用程序。应用需提供流畅的用户体验，让用户可以轻松管理歌词库、浏览和学习单词。

### 1.2. 核心技术栈

- **元框架**: Quasar Framework (v2)
- **核心框架**: Vue.js (v3) with Composition API
- **语言**: TypeScript
- **构建工具**: Vite
- **包管理器**: pnpm
- **API 通信**: Axios
- **状态管理**: Pinia
- **UI 设计**: 现代简约风格，遵循下述设计规范

### 1.3. 设计规范与风格指南

- **布局**: 干净整洁，网格对齐，大量留白，强调呼吸感。
- **色彩**:
  - **主背景**: `#FFFFFF` (纯白) 或 `#F9F9F9` (极浅灰)
  - **次级背景/面板**: `#F5F5F7` (苹果风格浅灰)
  - **主文字**: `#1D1D1F` (深灰，避免纯黑)
  - **次级文字/图标**: `#86868B` (中度灰)
  - **强调色/主操作**: `#007AFF` (iOS 蓝) 或 `#34C759` (iOS 绿)
- **字体**: 无衬线细体。在 CSS 中设置 `font-family` 优先级：
  ```css
  font-family: -apple-system, BlinkMacSystemFont, "Inter", "Helvetica Neue", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  ```
- **元素**:
  - **边框**: 几乎不用或使用极细的 `#E5E5E5` 边框。
  - **阴影**: 完全不用。
  - **圆角**: 统一使用适度的圆角，如 `border-radius: 8px;`。
- **图标**: 线性、细线风格。Quasar 自带的 Material Icons (Outlined 版本) 非常适合。

## 2. 项目结构

项目遵循 Quasar CLI 生成的标准结构，并在此基础上进行模块化组织。

```
frontend/
|
├── public/                 # 公共静态资源
├── src/
|   ├── assets/             # 静态资源，如图片、字体
|   ├── boot/               # Quasar 启动文件 (配置 Axios, i18n 等)
|   ├── components/         # 全局可复用 Vue 组件
|   |   ├── WordDetail.vue  # (示例) 单词详情面板
|   |   └── ...
|   ├── css/                # 全局 CSS/SCSS 文件
|   |   └── app.scss        # 全局样式变量和基础样式
|   ├── layouts/            # 页面布局组件
|   |   └── MainLayout.vue  # 应用主布局 (包含头部、侧边栏等)
|   ├── pages/              # 页面级组件 (路由映射)
|   |   ├── IndexPage.vue   # 主页 (单词列表和详情)
|   |   └── ...
|   ├── router/             # Vue Router 配置
|   ├── services/           # API 服务层 (重要)
|   |   └── api/            # (自动生成) OpenAPI Codegen 生成的 API 代码
|   ├── store/              # Pinia 状态管理
|   |   ├── appStore.ts     # (示例) 管理全局应用状态
|   |   └── vocabularyStore.ts # 管理词汇表和歌曲数据
|   └── App.vue             # Vue 应用根组件
|
├── quasar.config.js      # Quasar 配置文件
└── package.json            # 项目依赖和脚本
```

## 3. OpenAPI 与 API 服务层

### 3.1. API 代码生成
项目根目录下应放置 `openapi.yaml` 文件。通过 `pnpm gen-api` 命令（需在 `package.json` 中配置），可以自动生成类型安全的 API 请求代码到 `src/services/api/` 目录。

- **`package.json` 配置**:
  ```json
  "scripts": {
    "gen-api": "openapi-typescript-codegen --input ../openapi.yaml --output ./src/services/api --client axios"
  }
  ```
- **使用**: 在 `store` 或组件中，可以直接导入并使用，例如 `import { VocabularyService } from 'src/services/api';`。

### 3.2. Axios 配置
在 `src/boot/axios.ts` 文件中配置 Axios 实例，设置基础 URL。

- **`src/boot/axios.ts`**:
  ```typescript
  import { boot } from 'quasar/wrappers';
  import axios, { AxiosInstance } from 'axios';

  declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
      $axios: AxiosInstance;
    }
  }

  const api = axios.create({ baseURL: 'http://localhost:8080' });

  export default boot(({ app }) => {
    app.config.globalProperties.$axios = api;
  });

  export { api };
  ```

## 4. 状态管理 (Pinia)

Pinia 用于管理全局共享的数据、状态和异步操作。

- **`store/vocabularyStore.ts`**:
  ```typescript
  import { defineStore } from 'pinia';
  import { VocabularyService, type Song, /* ...其他类型 */ } from 'src/services/api';

  export const useVocabularyStore = defineStore('vocabulary', {
    state: () => ({
      words: [] as string[],
      songs: [] as Song[],
      totalPages: 0,
      totalWords: 0,
      isLoading: false,
    }),
    actions: {
      async fetchWords(page: number, size: number, prefix?: string) {
        this.isLoading = true;
        try {
          const wordPage = await VocabularyService.getWordList(prefix, page, size);
          this.words = wordPage.content;
          this.totalPages = wordPage.totalPages;
          this.totalWords = wordPage.totalElements;
        } catch (error) {
          // 处理错误，例如弹窗通知
        } finally {
          this.isLoading = false;
        }
      },
      // ... fetchSongs, importSongs 等其他 actions
    },
  });
  ```

## 5. UI 实现详解

### 5.1. 主布局 (`layouts/MainLayout.vue`)
这是应用的整体框架，包含左侧的单词列表和右侧的内容显示区。

- **布局结构**:
  - 使用 Quasar 的 `QLayout` 和 `QPageContainer`作为根。
  - 使用 `QSplitter` 组件来实现可拖拽的左右分栏布局。
    - **左侧面板 (`<template v-slot:before>`)**:
      - 顶部是一个 `QInput` 作为搜索框，绑定 `search_var`。
      - 中间是一个 `QList` 或 `QVirtualScroll` (当单词量巨大时性能更好)，用于显示单词列表。
      - 底部是一个 `QPagination` 组件，用于分页。
    - **右侧面板 (`<template v-slot:after>`)**:
      - 同样使用 `QSplitter` (设置为 `horizontal`) 分为上下两部分。
      - **上部**: "Occurrences" 面板，用于显示单词出现位置。
      - **下部**: "Dictionary" 面板，用于显示词典释义。

- **代码示例 (`MainLayout.vue`)**:
  ```vue
  <template>
    <q-layout view="hHh lpR fFf">
      <q-page-container>
        <q-page class="q-pa-md">
          <q-splitter v-model="splitterModel" style="height: calc(100vh - 82px);">
            <!-- 左侧单词列表 -->
            <template v-slot:before>
              <div class="q-pa-md">
                <q-input dense outlined v-model="searchTerm" label="Filter words..." clearable />
                <!-- 列表和分页 -->
              </div>
            </template>
            <!-- 右侧详情 -->
            <template v-slot:after>
              <!-- 上下分栏的 QSplitter -->
            </template>
          </q-splitter>
        </q-page>
      </q-page-container>
    </q-layout>
  </template>
  ```

### 5.2. 单词列表
- **组件**: 使用 `q-virtual-scroll` 应对大量数据。
- **数据**: 从 `vocabularyStore` 获取 `words` 数组。
- **交互**:
  - 监听 `QInput` 的 `update:model-value` 事件，触发 `vocabularyStore.fetchWords` 以实现模糊搜索。
  - 监听 `QPagination` 的 `update:model-value` 事件，触发 `vocabularyStore.fetchWords` 实现分页。
  - 列表项 (`QItem`) 点击时，触发一个事件，通知右侧面板更新显示的单词。

### 5.3. 单词详情面板
- **数据**: 接收从左侧列表传递过来的当前选中单词。
- **逻辑**:
  - `watch` 选中的单词变化，一旦变化，立即调用 `VocabularyService.getWordOccurrences` 和 `DictionaryService.lookupDictionaryWord`。
  - 将获取到的数据显示在对应的面板中。
  - 显示 Loading 状态 (使用 `QInnerLoading` 或 `QSkeleton`)。

### 5.4. 导入/管理对话框
- **触发**: 通过 `QHeader` 中的 `QBtn` 触发。
- **组件**: 使用 Quasar 的 `useDialogPluginComponent` 或简单的 `QDialog` 组件。
- **实现**: 复用 Python 版本 `LyricsBrowserDialog` 的 UI 逻辑，使用 Quasar 组件（如 `QTable` 代替 `Treeview`）来实现。
  - `QTable` 非常适合展示歌曲列表，并自带排序和过滤功能。
  - `QFile` 组件可以轻松实现文件选择。

## 6. 全局样式 (`css/app.scss`)

在这里定义符合我们设计规范的全局样式。

```scss
// src/css/app.scss

// 覆盖 Quasar 变量 (可选，但推荐)
$primary: #007AFF; // 强调色
$dark: #1D1D1F;   // 主文字颜色

// 全局字体
body {
  font-family: -apple-system, BlinkMacSystemFont, "Inter", "Helvetica Neue", /* ... */;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

// 移除默认阴影
.q-card, .q-toolbar, .q-drawer {
  box-shadow: none !important;
}

// 统一边框
.bordered {
  border: 1px solid #E5E5E5;
}

// 统一圆角
.rounded-borders {
  border-radius: 8px;
}
```
**配置**: 确保在 `quasar.config.js` 中引入此文件：
```js
// quasar.config.js
css: [
  'app.scss'
],
```

## 7. 部署与打包

- **桌面端**:
  - 在 `quasar.config.js` 中配置 `electron` 或 `tauri` 模式。推荐使用 **Tauri**。
  - 配置 Tauri 的 **Sidecar** 功能，使其在启动时自动运行后端的 `.jar` 文件。
  - 运行 `pnpm quasar build -m tauri`。
- **移动端**:
  - 运行 `pnpm quasar build -m capacitor -T android`。
  - 使用 Android Studio 打开生成的安卓项目 (`src-capacitor/android`) 并进行最终签名和打包。

---
