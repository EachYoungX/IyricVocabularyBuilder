<template>
  <q-dialog v-model="showDialog">
    <q-card
      style="min-width: 800px; width: 90vw; max-width: 1000px; display: flex; flex-direction: column; height: 80vh;">
      <!-- 顶部标题 -->
      <q-card-section class="row items-center q-pb-none col-auto">
        <div class="text-h6">{{ t('importDialogTitle') }}</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <q-card-section class="col column q-pa-none">
        <div class="q-pa-md col-auto">
          <!-- 1. 文件选择区 -->
          <div class="text-subtitle2 q-mb-sm">1. {{ t('chooseFiles') }}</div>
          <q-file v-model="selectedFiles" :label="t('dragDropFiles')" outlined dense multiple accept=".txt,.json,.lrc,.srt,.qrc"
            @update:model-value="handleFileSelect" class="q-mb-sm" use-chips>
            <template v-slot:prepend>
              <q-icon name="cloud_upload" />
            </template>
          </q-file>

          <!-- 2. 手动添加 (折叠) -->
          <q-expansion-item icon="playlist_add" :label="'2. ' + t('addSongManually')" class="bg-grey-1 rounded-borders">
            <q-card class="bg-grey-1">
              <q-card-section>
                <q-form @submit.prevent="addSongToList" class="row q-col-gutter-sm">
                  <div class="col-6">
                    <q-input v-model="newSong.title" :label="t('title') + ' *'" outlined dense bg-color="white" />
                  </div>
                  <div class="col-6">
                    <q-input v-model="newSong.artist" :label="t('artist') + ' *'" outlined dense bg-color="white" />
                  </div>
                  <div class="col-12">
                    <q-input v-model="newSong.lyrics" :label="t('lyrics') + ' *'" outlined dense type="textarea"
                      rows="3" bg-color="white" />
                  </div>
                  <div class="col-12 row justify-end">
                    <q-btn :label="t('addToPreviewList')" color="secondary" size="sm" type="submit"
                      :disable="!isSongValid" icon="add" />
                  </div>
                </q-form>
              </q-card-section>
            </q-card>
          </q-expansion-item>
        </div>

        <q-separator />

        <!-- [核心升级] 3. 预览与编辑列表 (可折叠) -->
        <q-expansion-item :label="`3. ${t('previewAndEdit')} (${songsToImport.length})`"
          :caption="t('checkDetailsBeforeImport')" icon="playlist_play" group="import-sections" default-opened
          header-class="bg-blue-1 text-primary" class="q-mb-none">
          <template v-slot:header>
            <q-item-section avatar>
              <q-icon name="playlist_play" color="primary" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-weight-bold">
                3. {{ t('previewAndEdit') }} ({{ songsToImport.length }})
              </q-item-label>
              <q-item-label caption>{{ t('checkDetailsBeforeImport') }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <!-- 折叠状态下的快速导入按钮 -->
              <q-btn v-if="songsToImport.length > 0" :label="t('importSongs', { count: songsToImport.length })"
                color="primary" size="sm" @click.stop="importSongs" :loading="isImporting"
                :disable="songsToImport.length === 0 || songsToImport.length > 100" icon="publish" class="q-mr-sm" />
              <q-btn v-if="songsToImport.length > 0" :label="t('clearAll')" flat color="negative" size="sm"
                @click.stop="songsToImport = []" />
            </q-item-section>
          </template>

          <q-card class="q-pa-md">
            <div class="row items-center justify-between q-mb-sm">
              <div class="text-subtitle1">
                {{ t('previewAndEdit') }} ({{ songsToImport.length }})
                <span class="text-caption text-grey-7 q-ml-sm">{{ t('checkDetailsBeforeImport') }}</span>
              </div>
              <q-btn v-if="songsToImport.length > 0" :label="t('clearAll')" flat color="negative" size="sm"
                @click="songsToImport = []" />
            </div>

            <!-- 滚动区域 -->
            <div class="scroll q-pr-sm" style="max-height: 400px;">
              <!-- 导入数量限制提示 -->
              <div v-if="songsToImport.length > 100"
                class="q-pa-sm bg-warning text-warning-dark rounded-borders q-mb-sm">
                <q-icon name="warning" class="q-mr-sm" />
                {{ t('importLimitExceeded', { max: 100 }) }}
                <q-btn :label="t('clearAll')" flat color="negative" size="sm" @click="songsToImport = []"
                  class="q-ml-sm" />
              </div>

              <div v-if="songsToImport.length === 0" class="text-center q-pa-lg text-grey">
                <q-icon name="library_music" size="4rem" color="grey-3" />
                <div class="q-mt-sm">{{ t('noSongsReadyForImport') }}</div>
              </div>

              <q-list separator bordered class="rounded-borders">
                <!-- 使用 Expansion Item 允许展开编辑 -->
                <q-expansion-item v-for="(song, index) in songsToImport" :key="index" group="songs"
                  header-class="bg-grey-1" expand-icon-class="text-primary">
                  <!-- 自定义头部：显示标题歌手 + 删除按钮 -->
                  <template v-slot:header>
                    <q-item-section avatar>
                      <q-avatar icon="music_note" color="primary" text-color="white" size="sm" font-size="18px" />
                    </q-item-section>

                    <q-item-section>
                      <q-item-label class="text-weight-bold">{{ song.title }}</q-item-label>
                      <q-item-label caption>{{ song.artist }}</q-item-label>
                      <q-item-label caption class="text-secondary" v-if="song.isNonEnglish">
                        <q-icon name="warning" size="14px" />
                        {{ t('nonEnglishLyricsDetected') }}
                      </q-item-label>
                      <div class="q-mt-xs row q-gutter-xs" v-if="song.importSummary">
                        <q-chip dense size="sm" color="primary" text-color="white" v-if="song.sourceFormat">
                          {{ song.sourceFormat }}
                        </q-chip>
                        <q-chip dense size="sm" color="grey-2">
                          {{ t('lyricLineCount', { count: song.importSummary.totalLines }) }}
                        </q-chip>
                        <q-chip dense size="sm" color="secondary" text-color="white">
                          {{ t('recognizedLyricLines', { count: song.importSummary.lyricLines }) }}
                        </q-chip>
                        <q-chip dense size="sm" color="accent" text-color="primary" v-if="song.importSummary.hiddenLines > 0">
                          {{ t('hiddenLineCount', { count: song.importSummary.hiddenLines }) }}
                        </q-chip>
                        <q-chip dense size="sm" color="red-1" text-color="red-9" v-if="song.importSummary.unknownLines > 0">
                          {{ t('unknownLineCount', { count: song.importSummary.unknownLines }) }}
                        </q-chip>
                      </div>
                    </q-item-section>

                    <q-item-section side>
                      <!-- 防止点击删除时触发折叠，使用 @click.stop -->
                      <q-btn flat round dense color="negative" icon="delete" size="sm"
                        @click.stop="removeSongFromList(index)">
                        <q-tooltip>{{ t('removeThisSong') }}</q-tooltip>
                      </q-btn>
                    </q-item-section>
                  </template>

                  <!-- 展开的内容：完全可编辑的表单 -->
                  <q-card>
                    <q-card-section class="q-gutter-y-sm">
                      <div class="row q-col-gutter-sm">
                        <div class="col-12 col-sm-6">
                          <q-input v-model="song.title" :label="t('title')" dense outlined />
                        </div>
                        <div class="col-12 col-sm-6">
                          <q-input v-model="song.artist" :label="t('artist')" dense outlined />
                        </div>
                      </div>
                      <q-input v-model="song.lyrics" :label="t('lyricsPreview')" outlined dense type="textarea" autogrow
                        input-style="min-height: 100px; max-height: 300px;" />
                      <q-banner v-if="song.importSummary" rounded class="bg-blue-1 text-blue-10">
                        {{ formatImportSummary(song.importSummary) }}
                      </q-banner>
                    </q-card-section>
                  </q-card>
                </q-expansion-item>
              </q-list>
            </div>
          </q-card>
        </q-expansion-item>
      </q-card-section>

      <!-- 底部固定操作栏 -->
      <q-card-actions align="right" class="bg-white text-primary q-pa-md bordered-top">
        <div class="text-caption text-grey q-mr-md" v-if="songsToImport.length > 0">
          {{ t('songsReady', { count: songsToImport.length }) }}
          <span v-if="songsToImport.length > 100" class="text-warning q-ml-sm">
            <q-icon name="warning" size="14px" />
            {{ t('importLimitExceeded', { max: 100 }) }}
          </span>
        </div>
        <q-btn :label="t('cancel')" flat v-close-popup />
        <q-btn :label="t('importSongs', { count: songsToImport.length })" color="primary" icon="publish"
          @click="importSongs" :loading="isImporting"
          :disable="songsToImport.length === 0 || songsToImport.length > 100" />
      </q-card-actions>

      <q-dialog v-model="showProgressDialog">
        <q-card style="width: 400px">
          <q-card-section>
            <div class="text-h6">{{ t('importing') }}</div>
          </q-card-section>
          <q-card-section>
            <q-linear-progress :value="importProgress" color="primary" size="10px" rounded class="q-mb-sm" />
            <div class="row justify-between text-caption">
              <span>{{ t('success') }}: {{ importTask?.successCount || 0 }}</span>
              <span class="text-negative" v-if="importTask?.failedCount">{{ t('failed') }}: {{ importTask?.failedCount
              }}</span>
            </div>
          </q-card-section>

          <!-- 失败列表 -->
          <q-card-section v-if="importTask?.failedItems?.length" style="max-height: 200px; overflow-y: auto;">
            <div class="text-negative text-weight-bold q-mb-xs">{{ t('failures') }}:</div>
            <div v-for="(fail, i) in importTask.failedItems" :key="i"
              class="text-caption text-grey-8 q-mb-xs border-bottom">
              • {{ fail.title }}: {{ fail.error }}
            </div>
          </q-card-section>

          <q-card-actions align="right" v-if="!isImporting">
            <q-btn :label="t('done')" color="primary" v-close-popup @click="handleImportDone" />
          </q-card-actions>
        </q-card>
      </q-dialog>

    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Notify } from 'quasar'
import { useI18n } from 'vue-i18n'
import { useSongsStore } from 'src/stores/songsStore'
import type { SongImportRequest, ImportTaskResult } from 'src/services/api'
import { ImportTaskResult as ImportTaskResultEnum } from 'src/services/api/models/ImportTaskResult'
import { detectLyricsLanguage } from 'src/utils/languageDetector'
import type { ExtendedSongImportRequest } from 'src/types/songImport'
import type { LyricImportSummary } from 'src/types/songImport'
import { buildImportSummary, parseImportFileContent } from 'src/utils/lyricsImportParser'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const songsStore = useSongsStore()

const showDialog = ref(props.modelValue)

// 监听props变化，更新showDialog
watch(() => props.modelValue, (newValue) => {
  showDialog.value = newValue
})

// 监听showDialog变化，emit事件
watch(showDialog, (newValue) => {
  emit('update:modelValue', newValue)
})

const selectedFiles = ref<File[] | null>(null)
const newSong = ref<SongImportRequest>({ title: '', artist: '', lyrics: '' })
// 这是所有待导入歌曲的列表，支持双向绑定编辑
const songsToImport = ref<ExtendedSongImportRequest[]>([])

const isImporting = ref(false)
const importTask = ref<ImportTaskResult | null>(null)
const taskId = ref<string | null>(null)

// 控制进度条弹窗
const showProgressDialog = ref(false)

// 监听importTask变化，更新进度对话框显示状态
watch(() => !!importTask.value, (newValue) => {
  showProgressDialog.value = newValue
})

const isSongValid = computed(() =>
  newSong.value.title.trim() !== '' &&
  newSong.value.artist.trim() !== '' &&
  newSong.value.lyrics.trim() !== ''
)

const importProgress = computed(() => {
  if (!importTask.value) return 0
  const processed = (importTask.value.successCount ?? 0) + (importTask.value.failedCount ?? 0)
  return importTask.value.total ? processed / importTask.value.total : 0
})

// ==================== 文件处理 (包含智能解析) ====================
async function handleFileSelect(files: File[] | null) {
  if (!files?.length) {
    selectedFiles.value = null
    return
  }

  const totalFiles = files.length
  let totalSongs = 0
  const newlyParsedSongs: ExtendedSongImportRequest[] = []
  const nonEnglishFiles: string[] = []

  // 弹一个"正在解析"的通知（只弹一次）
  Notify.create({
    group: 'file-parsing',
    spinner: true,
    message: t('parsingFiles', { count: totalFiles }),
    timeout: 0,
    type: 'ongoing'
  })

  try {
    await Promise.all(
      Array.from(files).map(async (file) => {
        try {
          const parsed = await parseFile(file)
          newlyParsedSongs.push(...parsed)
          totalSongs += parsed.length

          // 检测非英语
          if (parsed.some(s => s.isNonEnglish)) {
            nonEnglishFiles.push(file.name)
          }
        } catch (err) {
          // 单个文件失败不影响其他
          console.warn(`解析失败: ${file.name}`, err)
        }
      })
    )

    // 全部解析完毕 → 替换为最终通知（只弹一次！）
    let message = t('filesParsedSuccessfully', { songCount: totalSongs, fileCount: totalFiles })
    if (nonEnglishFiles.length > 0) {
      message += t('nonEnglishFilesDetected', { count: nonEnglishFiles.length })
    }

    Notify.create({
      group: 'file-parsing',  // 关键：同组 → 会替换掉上一个！
      type: 'positive',
      icon: 'check_circle',
      message,
      timeout: totalSongs > 20 ? 6000 : 4000,
      actions: [{ label: t('gotIt'), color: 'white' }]
    })

    // 更新待导入列表
    if (newlyParsedSongs.length > 0) {
      songsToImport.value = [...songsToImport.value, ...newlyParsedSongs]
    }

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : t('unknownError')
    Notify.create({
      group: 'file-parsing',
      type: 'negative',
      message: t('parsingError'),
      caption: errorMessage,
      timeout: 5000,
      actions: [{ label: t('gotIt'), color: 'white' }]
    })
  } finally {
    selectedFiles.value = null
  }
}

async function parseFile(file: File): Promise<ExtendedSongImportRequest[]> {
  const content = await file.text();
  return parseImportFileContent(file.name, content, t).map(attachLanguageDetection);
}

function attachLanguageDetection(song: ExtendedSongImportRequest): ExtendedSongImportRequest {
  if (song.lyrics) {
    const languageResult = detectLyricsLanguage(song.lyrics, t);
    if (languageResult.isNonEnglish) {
      song.isNonEnglish = true;
      song.languageWarning = languageResult.warning;
      song.languageConfidence = languageResult.confidence;
    }
  }
  return song;
}

function formatImportSummary(summary: LyricImportSummary) {
  return [
    t('lyricLineCount', { count: summary.totalLines }),
    t('recognizedLyricLines', { count: summary.lyricLines }),
    t('sectionLabelCount', { count: summary.sectionLabels }),
    t('speakerLabelCount', { count: summary.speakerLabels }),
    t('performanceNoteCount', { count: summary.performanceNotes }),
    t('metadataLineCount', { count: summary.metadataLines }),
    t('hiddenLineCount', { count: summary.hiddenLines }),
    t('unknownLineCount', { count: summary.unknownLines }),
  ].join(' · ')
}

// ==================== 列表管理 ====================
function addSongToList() {
  if (!isSongValid.value) return

  const song: ExtendedSongImportRequest = {
    ...newSong.value,
    sourceFormat: 'MANUAL',
    importSummary: buildImportSummary(newSong.value.lyrics),
  }

  // 检测歌词语言
  attachLanguageDetection(song)

  songsToImport.value.push(song)
  newSong.value = { title: '', artist: '', lyrics: '' }
  Notify.create({ message: t('songAddedToPreviewList'), color: 'positive', timeout: 1000 })
}

function removeSongFromList(index: number) {
  songsToImport.value.splice(index, 1)
}

// ==================== 导入 & 轮询 ====================
async function importSongs() {
  const total = songsToImport.value.length
  if (total === 0) {
    Notify.create({
      type: 'warning',
      message: t('noSongsToImport'),
      timeout: 3000
    })
    return
  }

  // 检查最大导入数量限制
  if (total > 100) {
    Notify.create({
      type: 'warning',
      message: t('importLimitExceeded', { max: 100 }),
      caption: t('pleaseReduceSongs'),
      timeout: 5000
    })
    return
  }

  isImporting.value = true
  importTask.value = null
  taskId.value = null

  // 第1步：开始时弹一个带进度的通知
  Notify.create({
    group: 'import-progress',  // 关键：分组
    type: 'ongoing',
    icon: 'cloud_upload',
    spinner: true,
    message: t('importingSongs', { count: total }),
    timeout: 0, // 不自动关闭
  })

  try {
    // 只传递基础字段给API，过滤掉扩展字段
    const basicSongs = songsToImport.value.map(song => ({
      title: song.title,
      artist: song.artist,
      lyrics: song.lyrics
    }))
    const result: unknown = await songsStore.importSongs(basicSongs)

    let receivedTaskId: string | null = null
    if (typeof result === 'string') {
      receivedTaskId = result
    } else if (result && typeof result === 'object') {
      const obj = result as Record<string, unknown>
      receivedTaskId = (obj.taskId as string) || (obj.id as string) || (obj.task_id as string) || null
    }

    if (!receivedTaskId) throw new Error('No Task ID received from server')

    taskId.value = receivedTaskId

    // 初始化进度条状态
    importTask.value = {
      taskId: receivedTaskId,
      status: ImportTaskResultEnum.status.PENDING,
      total: songsToImport.value.length,
      successCount: 0,
      failedCount: 0,
      failedItems: []
    }

    pollTaskStatus(receivedTaskId)

  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Failed to start import'
    Notify.create({
      group: 'import-progress',
      type: 'negative',
      message: msg,
      timeout: 5000
    })
    Notify.create({
      group: 'import-progress',
      type: 'negative',
      message: t('importFailed'),
      timeout: 5000
    })
    isImporting.value = false
  }
}

function pollTaskStatus(currentTaskId: string) {
  const pollInterval = 1500
  let attempts = 0
  const maxAttempts = 120

  const poll = async () => {
    if (taskId.value !== currentTaskId) return

    attempts++
    try {
      const taskResult = await songsStore.checkImportTaskStatus(currentTaskId)
      if (taskResult) {
        importTask.value = taskResult

        const isFinished =
          taskResult.status === ImportTaskResultEnum.status.COMPLETED ||
          taskResult.status === ImportTaskResultEnum.status.FAILED

        if (isFinished) {
          isImporting.value = false

          // 导入完成后自动刷新歌曲列表
          if (taskResult.status === ImportTaskResultEnum.status.COMPLETED) {
            try {
              await songsStore.fetchAllSongs()
            } catch (error) {
              console.error('Failed to refresh songs list after import:', error)
            }
          }

          // 导入完成后替换进度通知为最终结果通知
          const success = taskResult.successCount || 0
          const failed = taskResult.failedCount || 0
          const nonEnglish = songsToImport.value.filter(s => s.isNonEnglish).length

          let message = t('importCompleted', { success })
          if (failed > 0) message += t('failedSongs', { failed })
          if (nonEnglish > 0) message += t('nonEnglishSongs', { nonEnglish })

          Notify.create({
            group: 'import-progress',  // 同组 → 会替换掉进度通知！
            type: failed === 0 ? 'positive' : 'warning',
            icon: failed === 0 ? 'check_circle' : 'warning',
            message,
            timeout: 6000,
            actions: [{ label: t('gotIt'), color: 'white' }]
          })

          // 如果全成功，可以自动关闭，但为了看清结果，最好保留弹窗让用户点 Done
          if (taskResult.status === ImportTaskResultEnum.status.COMPLETED && taskResult.failedCount === 0) {
            // 全成功自动关闭主窗口 (延迟一下)
            setTimeout(() => {
              handleImportDone() // 清理数据
            }, 1500)
          }
          return
        }
      }

      if (attempts < maxAttempts) {
        setTimeout(() => void poll(), pollInterval)
      } else {
        isImporting.value = false
        Notify.create({ type: 'warning', message: t('taskTimedOut') })
      }
    } catch {
      // 忽略网络抖动错误，继续重试，直到超时
      if (attempts >= maxAttempts) {
        isImporting.value = false
      } else {
        setTimeout(() => void poll(), pollInterval)
      }
    }
  }
  void poll()
}

// 导入完成后的清理
function handleImportDone() {
  // 只有当全部成功，或者用户手动点击 Done 时才清空
  // 如果有失败的，保留失败的在列表中？这里为了简化，直接全部清空并关闭
  // 更好的做法是：只过滤掉成功的，保留失败的在 songsToImport 列表里。

  if (importTask.value?.failedCount === 0) {
    songsToImport.value = []
    emit('update:modelValue', false)
    importTask.value = null // 关闭进度条弹窗
    // 注意：这里不再发送通知，因为 pollTaskStatus 中已经发送了最终结果通知
  } else {
    // 有失败的情况：关闭进度弹窗，保留主列表，但主列表目前没有标记谁失败了。
    // 简单处理：清空并关闭，用户在进度条弹窗已经看到了失败原因。
    songsToImport.value = []
    emit('update:modelValue', false)
    importTask.value = null
  }
}
</script>

<style scoped lang="scss">
.bordered-top {
  border-top: 1px solid var(--lv-line);
}

.border-bottom {
  border-bottom: 1px solid var(--lv-line);
}

:deep(.q-card) {
  background: var(--lv-surface-solid);
}

:deep(.q-stepper) {
  border-radius: var(--lv-radius-md);
  box-shadow: none;
}

:deep(.q-stepper__header) {
  border-bottom-color: var(--lv-line);
}

:deep(.q-item) {
  border-radius: var(--lv-radius-sm);
}
</style>
