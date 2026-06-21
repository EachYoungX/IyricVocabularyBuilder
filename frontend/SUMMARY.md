### 《Lyric Vocabulary Builder - 前端开发问题与解决方案总结》（Vue 3 + Quasar）

**版本**：v1.0  
**日期**：2025-11-22

#### 1. Quasar q-table 点击整行选中失效（最经典最隐蔽的坑）

**问题现象**：  
添加 `selection="single" selectable="row-click"` 后，点击整行无反应，仅勾选框有效。

**根本原因**：

- `v-model:selected` 绑定的是完整 `Song[]` 对象
- Quasar 内部使用 `row-key` 的值（如 `id`）进行相等性判断
- 对象引用不同 -> Quasar 认为“不相等” -> 拒绝选中

**解决方案**：

```vue
<q-table
  selection="single"
  selectable-rows
  v-model:selected="selectedSong"
  @row-click="onRowClick"
  ...
/>

<script setup>
const selectedSong = ref<Song[]>([])

async function onRowClick(_evt: any, row: Song) {
  if (selectedSong.value[0]?.id === row.id) return
  selectedSong.value = [row]  // 手动控制选中
  // 手动更新右侧表单
  Object.assign(editableSong.value, { id: row.id, ... })
}
</script>
```

**关键点**：

- 使用 `selectable-rows` + `@row-click` 手动接管
- 保留 `v-model:selected` 保证高亮正常
- 这是目前最稳定、最直观的实现方式

#### 2. 右侧编辑区布局崩溃（歌词框被挤压、按钮消失）

**解决方案**：使用 `q-scroll-area` + 固定底部按钮区

```vue
<template v-slot:after>
  <div class="q-pa-md fit column">
    <q-scroll-area class="col">
      <q-form class="q-gutter-md">
        <q-input v-model="editableSong.title" />
        <q-input v-model="editableSong.artist" />
        <q-input v-model="editableSong.lyrics" type="textarea" class="full-height" />
      </q-form>
    </q-scroll-area>

    <!-- 固定底部按钮 -->
    <div class="q-pa-md border-top bg-white">
      <q-btn label="Delete Song" color="negative" flat />
      <q-btn label="Save Changes" color="primary" :disable="!isFormDirty" />
    </div>
  </div>
</template>
```

```scss
.full-height :deep(textarea) {
  height: 100% !important;
  min-height: 400px;
}
```

#### 3. Vue 响应式陷阱：歌词不显示的根本原因

**问题**：`console.log(editableSong.lyrics)` 有值，但 `<q-input>` 不显示

**根本原因**：

```ts
const editableSong = ref<SongUpdateRequest & { id?: number }>({});
// 初始为空对象 → Vue 不会为 lyrics 建立响应式依赖
editableSong.value = newSong; // 整对象替换 → 响应式丢失
```

**终极解决方案**：

```ts
// 方案1（推荐）：初始化时提供所有字段
const editableSong = ref<EditableSong>({
  id: undefined,
  title: '',
  artist: '',
  lyrics: '',
});

// 方案2：只赋值属性，不整对象替换
editableSong.value.title = song.title ?? '';
editableSong.value.lyrics = song.lyrics ?? '';
```

#### 4. Quasar Notify 在异步回调中失效

**错误写法**：

```ts
const $q = useQuasar()
setTimeout(() => $q.notify(...)) // $q 失效
```

**正确写法**：

```ts
import { Notify } from 'quasar';
Notify.create({ message: 'Success', color: 'positive' });
```

#### 5. 其他关键最佳实践

| 问题                                    | 解决方案                                        |
| --------------------------------------- | ----------------------------------------------- | ---------- |
| `exactOptionalPropertyTypes: true` 报错 | 关闭该选项或显式写 `id?: number                 | undefined` |
| `q-table ref` 调用方法报错              | 使用 `ref<QTable>(null!)` 并导入类型            |
| 多文件导入只显示最后一个                | 使用 `[...array.value, ...newItems]` 触发强更新 |

---

#### 6. 词汇页外边框显示正常但内容下沉、分页器被裁掉（最隐蔽的布局坑）

**问题现象**：  
词汇页外边框显示正常，但内容整体下沉、分页器无法完整显示（被裁掉或滚不到）

**根因排查顺序与最终结论**：

1. **先怀疑页面自身高度写死 `calc(100vh - 82px)`** → 排除
2. **怀疑 app.css 中 `.full-height { height: 100% !important; }`** → 注释后仍存在 → 排除
3. **最终锁定 App.vue 中的全局样式为真凶**，特别是以下代码：

```scss
.q-page-container {
  overflow: hidden;
}
.q-page {
  max-height: 100dvh;
  overflow-y: auto;
}
```

这几行与页面使用的 `window-height` + `flex` 布局 + `overflow-hidden` 产生致命冲突：

- 强制把 `q-page` 高度锁死为 `100dvh`
- 顶部语言按钮再占用固定高度 → 总高度 > 100dvh
- 超出部分被 `overflow-hidden` 直接裁掉 → 分页器永远在可视区外

**解决方法（一次性根治）**：
将 App.vue 全局样式全部替换为现代 flex 布局标准写法：

```scss
html,
body,
#q-app {
  height: 100%;
  overflow: hidden;
}

.q-page-container {
  height: 100dvh;
  display: flex;
  flex-direction: column;
}

.q-page {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.window-height {
  height: 100dvh !important;
  display: flex !important;
  flex-direction: column !important;
}
```

同时删除 App.vue 中所有强制修改 `q-splitter` 方向和高度的 `@media` 样式（已交由各页面自行控制）

**最终效果**：

- 外边框完整显示（与歌曲管理页一致）
- 分页器永远可见且可点击
- 移动端/桌面端高度自适应完美
- 不再依赖任何 `calc(100vh - xx)` 硬编码
- 后续新增顶部工具栏也无需手动调整高度

**经验教训**：
全局强制 `max-height: 100dvh` + `overflow-hidden` 是 Quasar Vue3 项目中最常见的"内容被裁掉"元凶，必须用 `flex` + `min-height: 0` 方案彻底取代。
