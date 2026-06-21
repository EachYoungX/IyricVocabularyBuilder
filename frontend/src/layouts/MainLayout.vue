<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="bg-white text-dark">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />

        <q-toolbar-title class="text-weight-bold">{{ t('appTitle') }}</q-toolbar-title>


      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" show-if-above bordered :width="260" class="bg-grey-1">
      <!-- Logo / 标题区（可选，更高级） -->
      <div class="q-pa-lg border-b border-grey-3">
        <div class="text-h6 text-weight-bold text-grey-8">
          {{ t('appTitle') }}
        </div>
        <div class="text-caption text-grey-6">{{ t('appSubtitle') }}</div>
      </div>

      <q-list class="q-mt-md">
        <!-- 标题 -->
        <q-item-label header class="text-overline text-grey-6 q-px-lg q-mb-2">
          {{ t('navigation') }}
        </q-item-label>

        <!-- Vocabulary 主页 -->
        <q-item to="/" exact clickable v-ripple class="q-mx-md q-mb-1 rounded-borders"
          active-class="bg-primary text-white">
          <q-item-section avatar>
            <q-icon name="o_class" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('home') }}</q-item-label>
          </q-item-section>
        </q-item>

        <!-- 歌曲管理 -->
        <q-item to="/songs" clickable v-ripple class="q-mx-md q-mb-1 rounded-borders"
          active-class="bg-primary text-white">
          <q-item-section avatar>
            <q-icon name="o_library_music" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('songsManager') }}</q-item-label>
          </q-item-section>
        </q-item>

        <!-- 分隔线 + 功能区（可选） -->
        <q-separator spaced inset class="q-my-lg" />

        <q-item-label header class="text-overline text-grey-6 q-px-lg q-mb-2">
          {{ t('actions') }}
        </q-item-label>

        <q-item clickable v-ripple class="q-mx-md q-mb-1 rounded-borders" @click="showImportDialog">
          <q-item-section avatar>
            <q-icon name="add" color="positive" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('importSongs') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable v-ripple class="q-mx-md rounded-borders" @click="refreshVocabulary">
          <q-item-section avatar>
            <q-icon name="refresh" color="primary" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('refreshIndex') }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>

    <SongImportDialog v-model="importDialogVisible" />
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Loading, Notify } from 'quasar'
import { VocabularyService } from 'src/services/api'
import { ImportTaskResult as ImportTaskResultEnum } from 'src/services/api/models/ImportTaskResult'
import SongImportDialog from 'components/SongImportDialog.vue'
import { useVocabularyStore } from 'src/stores/vocabularyStore'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'

const $q = useQuasar()
const { t } = useI18n()

// --- 组件状态 ---
const leftDrawerOpen = ref(false)
const importDialogVisible = ref(false)

// --- Pinia Store ---
const vocabularyStore = useVocabularyStore()

// --- UI 控制函数 ---
function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

function showImportDialog() {
  importDialogVisible.value = true
}

// --- [核心] 词汇刷新逻辑 ---
// 1. 用户点击左侧「 Tear Down Index」时，只弹出对话框
function refreshVocabulary() {
  // 直接弹出 Quasar 美美的对话框
  $q.dialog({
    title: t('tearDownIndexTitle'),
    message: t('tearDownIndexMessage'),
    html: true,
    ok: {
      label: t('tearDownNow'),
      color: 'primary',
      unelevated: true
    },
    cancel: {
      label: t('cancel'),
      flat: true,
      color: 'grey'
    },
    persistent: true
  }).onOk(() => {
    void startVocabularyRebuild()  // 用户确认后才真正开始
  })
}

// 2. 真正的重建逻辑抽离出来（原来 refreshVocabulary 的内容）
async function startVocabularyRebuild() {
  if (Loading.isActive) {
    Notify.create({ message: t('rebuildInProgress'), type: 'info' })
    return
  }

  Loading.show({
    message: t('startingRebuild'),
    spinnerColor: 'primary'
  })

  try {
    const response = await VocabularyService.refreshVocabularyIndex()
    if (response?.taskId) {
      await pollRefreshStatus(response.taskId)
    } else {
      throw new Error(t('noTaskIdReturned'))
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : t('unknownError')
    Notify.create({
      type: 'negative',
      message: `${t('rebuildFailed')}: ${msg}`,
      icon: 'error'
    })
  } finally {
    Loading.hide()
  }
}

function pollRefreshStatus(taskId: string): Promise<void> {
  // [核心修复] 函数现在返回一个 Promise
  return new Promise((resolve, reject) => {
    const pollInterval = 2000
    const maxAttempts = 90
    let attempts = 0

    const poll = async () => {
      attempts++

      try {
        const taskResult = await VocabularyService.getRefreshTaskStatus(taskId)

        // 更新加载信息 (可选，但体验很好)
        if (Loading.isActive && taskResult?.total) {
          const processed = (taskResult.successCount ?? 0) + (taskResult.failedCount ?? 0)
          const progress = taskResult.total > 0 ? (processed / taskResult.total * 100).toFixed(0) : 0
          Loading.show({
            message: t('rebuildingIndexProgress', { progress })
          })
        }

        const isFinished = taskResult?.status === ImportTaskResultEnum.status.COMPLETED ||
          taskResult?.status === ImportTaskResultEnum.status.FAILED

        if (isFinished) {
          if (taskResult.status === ImportTaskResultEnum.status.COMPLETED) {
            Notify.create({
              message: t('rebuildSuccess'),
              type: 'positive', icon: 'check_circle', position: 'top'
            })
            // 调用 store action 刷新前端数据
            await vocabularyStore.fetchWords({ page: 0, size: 50 })
            resolve() // 成功完成，解决 Promise
          } else {
            Notify.create({
              message: t('rebuildTaskFailed'),
              type: 'negative', icon: 'error', position: 'top'
            })
            reject(new Error(t('rebuildTaskFailedOnServer'))) // 失败，拒绝 Promise
          }
          return
        }

        if (attempts >= maxAttempts) {
          Notify.create({ message: t('rebuildTaskTimeout'), type: 'warning', icon: 'timer_off' })
          reject(new Error(t('refreshTaskTimedOut'))) // 超时，拒绝 Promise
          return
        }

        setTimeout(() => void poll(), pollInterval)
      } catch {
        Notify.create({ type: 'negative', message: t('checkStatusFailed'), icon: 'cloud_off' })
        reject(new Error(t('failedToCheckRefreshStatus'))) // 发生连接错误等，拒绝 Promise
      }
    }

    void poll()
  })
}
</script>
