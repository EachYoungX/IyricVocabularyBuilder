<template>
  <q-page class="window-height column no-wrap overflow-hidden q-pa-sm-md">
    <!-- 顶部标题 -->
    <div class="page-title serif-display q-mb-sm col-auto q-px-sm">{{ t('songsManagerTitle') }}</div>

    <!-- 主体容器 -->
    <div class="col full-width" style="display: flex">
      <q-splitter v-model="splitterModel" :horizontal="$q.screen.lt.md" class="fit desktop-splitter" unit="%"
        :limits="[10, 90]">

        <!-- 左侧/上方：歌曲列表 -->
        <template v-slot:before>
          <div class="column no-wrap fit">
            <!-- 工具栏 (固定高度) -->
            <div class="q-pa-sm col-auto song-toolbar">
              <q-input v-model="filter" dense outlined clearable :label="t('searchPlaceholder')" class="q-mb-sm" />
              <div class="row items-center q-gutter-sm">
                <template v-if="isBatchMode">
                  <q-btn color="grey-7" icon="close" :label="t('cancel')" size="sm" @click="disableBatchMode"
                    unelevated />
                  <q-btn color="negative" icon="delete_forever" :label="t('deleteSelected')" size="sm"
                    :disable="selectedSongs.length === 0" @click="confirmBatchDelete" :loading="deletingBatch"
                    unelevated />
                </template>
                <q-btn v-else color="grey-7" icon="playlist_remove" :label="t('selectAll')" size="sm"
                  @click="enableBatchMode" unelevated />
              </div>
            </div>

            <!-- 表格容器 - 添加padding类 -->
            <div class="col song-list-container"
              style="min-height: 0; width: 100%; max-width: 100%; display: flex; flex-direction: column; overflow: hidden;">
              <q-table ref="songsTable" flat bordered :rows="songsStore.songs" :columns="columns" row-key="id"
                :filter="filter" :loading="songsStore.isLoading" :selection="selectionMode"
                v-model:selected="selectedSongs" selectable-rows @row-click="onRowClick" :rows-per-page-options="[0]"
                hide-bottom class="my-sticky-header-table" virtual-scroll :virtual-scroll-sticky-size-start="0" />
            </div>
          </div>
        </template>

        <!-- 右侧/下方：详情预览 -->
        <template v-slot:after>
          <div class="q-pa-md fit column no-wrap song-detail-surface">

            <!-- 空状态 -->
            <div v-if="!selectedSongs.length" class="col column flex-center text-center">
              <q-icon name="queue_music" size="50px" color="grey-4" class="q-mb-md" />
              <div class="text-h6 text-grey-7 q-mb-sm">{{ t('noSelection') }}</div>
              <div class="text-caption text-grey-6">{{ t('selectSongToView') }}</div>
            </div>

            <!-- 歌曲详情 -->
            <div v-else-if="!isBatchMode || selectedSongs.length === 1" class="col column q-gutter-y-md song-detail-panel">
              <div class="text-subtitle1 col-auto">{{ t('songDetails') }}</div>

              <q-card flat bordered class="song-detail-card">
                <q-card-section>
                  <div class="text-caption text-grey-7">{{ t('title') }}</div>
                  <div class="text-h6 serif-display">{{ selectedSong?.title || t('untitledSong') }}</div>
                </q-card-section>
                <q-separator />
                <q-card-section>
                  <div class="text-caption text-grey-7">{{ t('artist') }}</div>
                  <div class="text-body1">{{ selectedSong?.artist || t('unknown') }}</div>
                </q-card-section>
                <q-separator />
                <q-card-section class="col song-lyrics-section">
                  <div class="text-caption text-grey-7 q-mb-xs">{{ t('lyricsPreview') }}</div>
                  <pre class="song-lyrics-preview">{{ selectedSong?.lyrics || t('emptyLyrics') }}</pre>
                </q-card-section>
              </q-card>

              <div class="song-detail-actions row q-gutter-sm justify-end col-auto q-pt-sm">
                <q-btn :label="t('delete')" color="negative" outline no-caps icon="delete_forever" @click="confirmDelete" />
                <q-btn :label="t('editLyrics')" color="secondary" outline no-caps icon="edit_note"
                  @click="lyricEditorVisible = true" />
              </div>
            </div>

            <!-- 批量选择状态 -->
            <div v-else class="col column flex-center text-center">
              <q-icon name="playlist_remove" size="50px" color="secondary" class="q-mb-md" />
              <div class="text-h6 text-grey-7">
                {{ selectedSongs.length }} {{ t('selected') }}
              </div>
              <q-space />
              <div class="row q-gutter-sm justify-center q-pb-md">
                <q-btn color="grey-7" icon="close" :label="t('cancel')" @click="disableBatchMode" unelevated
                  size="sm" />
                <q-btn color="negative" icon="delete_forever" :label="t('deleteSelected')" @click="confirmBatchDelete"
                  unelevated size="sm" :loading="deletingBatch" />
              </div>
            </div>
          </div>
        </template>

      </q-splitter>
    </div>
    <LyricEditDialog
      v-model="lyricEditorVisible"
      :song-id="selectedSong?.id"
      :song-title="selectedSong?.title"
      @saved="handleLyricEditorSaved"
    />
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { QTableColumn, QTable } from 'quasar';
import { useQuasar } from 'quasar'
import { useSongsStore } from 'src/stores/songsStore'
import { Notify } from 'quasar'
import type { Song } from 'src/services/api'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import LyricEditDialog from 'components/lyric/LyricEditDialog.vue'

const $q = useQuasar()
const songsStore = useSongsStore()
const route = useRoute()
const { t } = useI18n()
const songsTable = ref<QTable | null>(null)
const splitterModel = ref(50)
const filter = ref('')
const lyricEditorVisible = ref(false)

// 表格列
const columns: QTableColumn[] = [
  {
    name: 'title',
    label: t('title'),
    field: 'title',
    sortable: true,
    align: 'left',
    style: 'min-width: 160px; max-width: 300px; white-space: normal;'
  },
  {
    name: 'artist',
    label: t('artist'),
    field: 'artist',
    sortable: true,
    align: 'left',
    style: 'min-width: 120px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'
  },
]

// 选择模式管理
const isBatchMode = ref(false)
const selectionMode = computed(() => isBatchMode.value ? 'multiple' : 'single')

// 选中状态
const selectedSongs = ref<Song[]>([])

// 批量删除加载状态
const deletingBatch = ref(false)

const selectedSong = computed(() => selectedSongs.value[0])

// 启用批量模式
const enableBatchMode = () => {
  isBatchMode.value = true
  // 保持当前选中的歌曲（如果有）
  if (selectedSongs.value.length > 0) {
    selectedSongs.value = [...selectedSongs.value]
  }
}

// 禁用批量模式
const disableBatchMode = () => {
  isBatchMode.value = false
  selectedSongs.value = []
}

// 当选中变化时，普通模式只保留第一首。
watch(() => selectedSongs.value, (newSelection: Song[]) => {
  if (newSelection.length > 1 && !isBatchMode.value) {
    // 在多选时（非批量模式）只保留第一个选中的
    const firstSong = newSelection[0]
    if (firstSong) {
      selectedSongs.value = [firstSong]
    }
  }
}, { immediate: true })

// 点击行处理
function onRowClick(_evt: Event, row: Song) {
  if (isBatchMode.value) {
    // 批量模式下：切换选中状态
    const index = selectedSongs.value.findIndex((s) => s.id === row.id)
    if (index > -1) {
      selectedSongs.value.splice(index, 1)
    } else {
      selectedSongs.value.push(row)
    }
    selectedSongs.value = [...selectedSongs.value] // 触发更新
  } else {
    // 普通模式下：单选
    const currentSong = selectedSongs.value[0]
    if (currentSong && currentSong.id === row.id) {
      // 点击已选中的行，取消选择
      selectedSongs.value = []
    } else {
      // 选择新行
      selectedSongs.value = [row]
    }
  }
}

function handleLyricEditorSaved(song: Song) {
  selectedSongs.value = [song]
}

// 单曲删除
const confirmDelete = () => {
  const song = selectedSongs.value[0]
  if (!song) return
  $q.dialog({
    title: t('confirmDeletion'),
    message: t('permanentlyDeleteSong', { title: song.title || t('untitledSong') }),
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      await songsStore.deleteSong(song.id)
      selectedSongs.value = []
    })()
  })
}

// 批量删除
const confirmBatchDelete = () => {
  if (selectedSongs.value.length === 0) return

  const count = selectedSongs.value.length
  const titles = selectedSongs.value.slice(0, 3).map((s) => s.title || t('untitledSong')).join(', ')
  const message = count > 3
    ? t('permanentlyDeleteMultipleSongs', { count, titles })
    : t('permanentlyDeleteSongs', { count, titles })

  $q.dialog({
    title: t('confirmBatchDeletion'),
    message: `${message}<br><br><strong>${t('actionCannotBeUndone')}</strong>`,
    html: true,
    cancel: { label: t('cancel'), flat: true },
    ok: { label: t('deleteForever'), color: 'negative' },
    persistent: true,
  }).onOk(() => void batchDeleteSongs())
}

const batchDeleteSongs = async () => {
  const ids = selectedSongs.value.map((s) => s.id).filter(Boolean)
  if (ids.length === 0) return

  deletingBatch.value = true
  try {
    await songsStore.deleteSongsBatch(ids)
    Notify.create({
      type: 'positive',
      message: t('successfullyDeletedSongs', { count: ids.length }),
      icon: 'delete_forever',
      timeout: 4000,
    })
    selectedSongs.value = []
    isBatchMode.value = false // 退出批量模式
  } catch {
    Notify.create({
      type: 'negative',
      message: t('failedToDeleteSomeSongs'),
      icon: 'error',
    })
  } finally {
    deletingBatch.value = false
  }
}

// 初始化加载
async function initializeSongs() {
  if (songsStore.songs.length === 0) await songsStore.fetchAllSongs(false)
  const requestedSongId = Number(route.query.songId)
  if (Number.isFinite(requestedSongId)) {
    const song = songsStore.songs.find((item: Song) => item.id === requestedSongId)
    if (song) selectedSongs.value = [song]
  }
}

onMounted(() => {
  void initializeSongs()
})
</script>

<style lang="scss" scoped>
.page-title {
  color: var(--lv-ink);
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 700;
  line-height: 1;
}

.song-toolbar {
  border-bottom: 1px solid var(--lv-line);
  background: var(--lv-surface-solid);
}

.song-detail-surface {
  min-width: 0;
  overflow: hidden;
  background: var(--lv-page-bg);
}

/* 确保表格组件作为 Flex 子项正确填充 */
.my-sticky-header-table {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0 !important;
}

/* 针对 Quasar 表格内部结构的修复 */
:deep(.q-table__middle) {
  flex: 1;
  overflow: auto;
}

/* 歌曲列表容器 - 添加与工具栏一致的8px padding */
.song-list-container {
  padding: 8px;
  box-sizing: border-box;
}

.song-detail-panel {
  min-height: 0;
}

.song-detail-card {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: var(--lv-paper);
}

.song-lyrics-section {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.song-lyrics-preview {
  flex: 1;
  min-height: 0;
  max-height: min(52dvh, 520px);
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  color: var(--lv-ink);
  font-family: var(--lv-font-serif);
  line-height: 1.75;
}

.song-detail-actions {
  flex-wrap: wrap;
}

/* 响应式内边距 */
@media (max-width: 600px) {
  .q-pa-sm-md {
    padding: 8px;
  }

  .page-title {
    font-size: 28px;
  }

  .song-list-container {
    padding: 6px;
  }

  :deep(.q-splitter__before) {
    min-height: 38dvh;
  }

  :deep(.q-splitter__after) {
    min-height: 42dvh;
  }
}

@media (min-width: 601px) {
  .q-pa-sm-md {
    padding: 16px;
  }
}

/* 桌面端分割器样式 - 与旧版保持一致 */
@media (min-width: 768px) {
  .desktop-splitter {
    border: 1px solid var(--lv-line);
    border-radius: var(--lv-radius-md);
    background: var(--lv-surface-solid);
  }

  /* 隐藏桌面端的分隔线，让边框看起来是连续的 */
  :deep(.q-splitter__separator) {
    background-color: transparent;
    width: 1px;
    /* 保持很细的分隔线，但透明 */
  }

  /* 确保分隔区域仍然可拖动 */
  :deep(.q-splitter__separator-area) {
    cursor: col-resize;
    z-index: 10;
    background-color: transparent;

    /* 添加悬停效果，让用户知道这里可以拖动 */
    &:hover {
      background-color: rgba(27, 60, 83, 0.06);
    }
  }

  /* 为分割器面板添加内部边框，模拟旧版效果 */
  :deep(.q-splitter__panel) {
    &:first-child {
      border-right: 1px solid var(--lv-line);
    }
  }
}

/* 移动端分割器样式 */
@media (max-width: 767px) {
  .desktop-splitter {
    border: 1px solid var(--lv-line);
    border-radius: var(--lv-radius-md);
    background: var(--lv-surface-solid);
    overflow: hidden;
  }

  :deep(.q-splitter__separator) {
    background-color: var(--lv-line);

    /* 移动端添加水平分割线样式 */
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      height: 4px;
      width: 40px;
      background-color: var(--lv-line-strong);
      border-radius: 2px;
      opacity: 0.7;
    }
  }

  :deep(.q-splitter__separator-area) {
    cursor: row-resize;
    z-index: 10;
  }
}

@media (max-width: 900px) and (orientation: landscape) {
  .page-title {
    font-size: 26px;
  }

  :deep(.q-splitter__before) {
    min-height: 48dvh;
  }
}
</style>
