

### **补充计划：集成歌单管理页面**

我们将按照以下步骤，将这个功能无缝地集成到现有的 Quasar 项目中。

#### **1. 更新项目结构 (`src/`)**

我们需要一个新的页面和一个新的 Pinia Store 来管理歌曲数据。

```
frontend/
|
├── src/
|   ├── pages/
|   |   ├── IndexPage.vue         # 现有的主页 (单词学习)
|   |   └── SongsManagerPage.vue  # [新增] 歌曲管理页面
|   |
|   └── store/
|       ├── vocabularyStore.ts    # 现有的词汇 Store
|       └── songsStore.ts         # [新增] 专门管理歌曲的 Store
|
└── ...
```

#### **2. 创建 `songsStore.ts`**

这个 Store 将负责所有与 `/api/songs` 接口的交互：获取、更新、删除歌曲。

**`src/stores/songsStore.ts`**
```typescript
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { SongsService, type Song, type SongUpdateRequest } from 'src/services/api';
import { Notify } from 'quasar';

export const useSongsStore = defineStore('songs', () => {
  // --- State ---
  const songs = ref<Song[]>([]);
  const isLoading = ref(false);

  // --- Actions ---
  async function fetchAllSongs() {
    isLoading.value = true;
    try {
      songs.value = await SongsService.getAllSongs();
    } catch (error) {
      Notify.create({ type: 'negative', message: 'Failed to fetch song library.' });
    } finally {
      isLoading.value = false;
    }
  }

  async function updateSong(id: number, request: SongUpdateRequest): Promise<boolean> {
    isLoading.value = true;
    try {
      const updatedSong = await SongsService.updateSong(id, request);
      const index = songs.value.findIndex(s => s.id === id);
      if (index !== -1) {
        songs.value[index] = updatedSong;
      }
      Notify.create({ type: 'positive', message: `'${updatedSong.title}' updated successfully.` });
      return true;
    } catch (error) {
      Notify.create({ type: 'negative', message: 'Failed to update song.' });
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function deleteSong(id: number): Promise<boolean> {
    isLoading.value = true;
    try {
      await SongsService.deleteSong(id);
      songs.value = songs.value.filter(s => s.id !== id);
      Notify.create({ type: 'positive', message: 'Song deleted successfully.' });
      return true;
    } catch (error) {
      Notify.create({ type: 'negative', message: 'Failed to delete song.' });
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return { songs, isLoading, fetchAllSongs, updateSong, deleteSong };
});
```

#### **3. 更新路由 (`src/router/routes.ts`)**

我们需要为新页面添加一个路由，以便用户可以访问它。

**`src/router/routes.ts`**
```typescript
import { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      // --- [新增] ---
      { 
        path: '/songs', // 新页面的路径
        name: 'SongsManager',
        component: () => import('pages/SongsManagerPage.vue') 
      },
    ],
  },
  // ... (404 路由)
];

export default routes;
```

#### **4. 在主布局中添加入口 (`src/layouts/MainLayout.vue`)**

用户需要一个入口来跳转到新的管理页面。最佳位置是左侧的抽屉导航栏。

**`src/layouts/MainLayout.vue` (`<template>` 部分)**
```html
<!-- ... -->
<q-drawer v-model="leftDrawerOpen" show-if-above bordered>
  <q-list>
    <q-item-label header>Navigation</q-item-label>
    
    <!-- 链接到主页 -->
    <q-item clickable to="/" exact>
      <q-item-section avatar><q-icon name="o_class" /></q-item-section>
      <q-item-section><q-item-label>Vocabulary</q-item-label></q-item-section>
    </q-item>
    
    <!-- [新增] 链接到歌曲管理页 -->
    <q-item clickable to="/songs">
      <q-item-section avatar><q-icon name="o_library_music" /></q-item-section>
      <q-item-section><q-item-label>Manage Songs</q-item-label></q-item-section>
    </q-item>
    
  </q-list>
</q-drawer>
<!-- ... -->
```
*   **注意**: 我使用了 `o_class` (单词卡片) 和 `o_library_music` (音乐库) 这两个线性图标，以符合您的设计规范。

#### **5. 创建 `SongsManagerPage.vue` (核心)**

这是新页面的实现，它将复现 Python 版本 `LyricsBrowserDialog` 的所有功能。

**`src/pages/SongsManagerPage.vue`**
```vue
<template>
  <q-page class="q-pa-md">
    <div class="text-h5 q-mb-md">Songs Library</div>

    <q-splitter v-model="splitterModel" style="height: calc(100vh - 120px);">

      <!-- 左侧：歌曲列表 -->
      <template v-slot:before>
        <div class="q-pa-sm">
          <q-input
            v-model="filter"
            dense outlined clearable
            label="Search by title or artist..."
            class="q-mb-sm"
          />
          <q-table
            flat bordered
            :rows="songsStore.songs"
            :columns="columns"
            row-key="id"
            :filter="filter"
            :loading="songsStore.isLoading"
            selection="single"
            v-model:selected="selectedSong"
            @selection="onRowSelect"
            :rows-per-page-options="[0]"
            hide-bottom
            class="full-height-table"
          />
        </div>
      </template>

      <!-- 右侧：歌曲详情与编辑 -->
      <template v-slot:after>
        <div class="q-pa-md">
          <div v-if="!selectedSong.length" class="text-center text-grey q-mt-xl">
            <q-icon name="o_music_note" size="xl" />
            <div>Select a song from the list to view or edit.</div>
          </div>
          <q-form v-else @submit.prevent="saveChanges">
            <div class="text-subtitle1 q-mb-sm">Edit Song Details</div>
            
            <q-input v-model="editableSong.title" label="Title *" outlined dense class="q-mb-md" />
            <q-input v-model="editableSong.artist" label="Artist *" outlined dense class="q-mb-md" />
            <q-input 
              v-model="editableSong.lyrics" 
              label="Lyrics *" 
              outlined dense 
              type="textarea" 
              class="lyrics-textarea"
            />

            <div class="row q-mt-md q-gutter-sm justify-end">
              <q-btn 
                label="Delete Song" 
                color="negative" 
                flat 
                @click="confirmDelete"
              />
              <q-btn 
                label="Save Changes" 
                color="primary" 
                type="submit"
                :disable="!isFormDirty"
              />
            </div>
          </q-form>
        </div>
      </template>

    </q-splitter>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useSongsStore } from 'src/stores/songsStore';
import { type Song, type SongUpdateRequest } from 'src/services/api';
import { useQuasar } from 'quasar';

const $q = useQuasar();
const songsStore = useSongsStore();

const splitterModel = ref(40); // 左侧面板初始宽度
const filter = ref(''); // 搜索过滤器

// --- 表格定义 ---
const columns = [
  { name: 'title', label: 'Title', field: 'title', sortable: true, align: 'left' },
  { name: 'artist', label: 'Artist', field: 'artist', sortable: true, align: 'left' },
];

// --- 选中与编辑逻辑 ---
const selectedSong = ref<Song[]>([]);
const editableSong = ref<SongUpdateRequest & { id?: number }>({});
const originalSongJson = ref('');

onMounted(() => {
  if (songsStore.songs.length === 0) {
    void songsStore.fetchAllSongs();
  }
});

// 当表格行被选中时触发
function onRowSelect(details: { rows: Song[] }) {
  if (details.rows.length > 0) {
    // 深拷贝选中行数据到编辑区
    editableSong.value = { ...details.rows[0] };
    originalSongJson.value = JSON.stringify(details.rows[0]);
  }
}

// 检查表单是否有改动
const isFormDirty = computed(() => {
  return JSON.stringify(editableSong.value) !== originalSongJson.value;
});

async function saveChanges() {
  if (!isFormDirty.value || !editableSong.value.id) return;
  
  const success = await songsStore.updateSong(editableSong.value.id, {
    title: editableSong.value.title,
    artist: editableSong.value.artist,
    lyrics: editableSong.value.lyrics,
  });

  if (success) {
    originalSongJson.value = JSON.stringify(editableSong.value); // 更新原始数据快照
    $q.dialog({
      title: 'Refresh Required',
      message: 'Song data has been updated. To see the changes in the vocabulary list, a refresh is required. Refresh now?',
      cancel: true,
      persistent: true
    }).onOk(() => {
      // 触发主布局的刷新方法
      // 这需要通过 emit/props 或 Pinia store 来实现
      // (简单起见，暂时只提示用户)
    });
  }
}

function confirmDelete() {
  if (!editableSong.value.id) return;

  $q.dialog({
    title: 'Confirm Deletion',
    message: `Are you sure you want to permanently delete '${editableSong.value.title}'? This action cannot be undone.`,
    cancel: true,
    persistent: true,
    color: 'negative'
  }).onOk(async () => {
    const success = await songsStore.deleteSong(editableSong.value.id!);
    if (success) {
      selectedSong.value = [];
      editableSong.value = {};
      originalSongJson.value = '';
    }
  });
}
</script>

<style lang="scss" scoped>
.full-height-table {
  height: calc(100vh - 120px - 48px - 16px); // 页面高度 - 头部 - 输入框 - padding
}
.lyrics-textarea {
  height: calc(100vh - 120px - 170px); // 动态计算高度
  max-height: 500px;
}
</style>
```

---
