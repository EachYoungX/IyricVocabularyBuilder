<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="bg-white text-dark">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />

        <q-toolbar-title class="text-weight-bold">{{ t('appTitle') }}</q-toolbar-title>
        <q-btn flat dense :label="locale === 'zh-CN' ? 'English' : '中文'" @click="toggleLanguage" />
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

        <q-item clickable v-ripple class="q-mx-md rounded-borders" @click="requestVocabularyRebuild">
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
import SongImportDialog from 'components/SongImportDialog.vue'
import { useI18n } from 'vue-i18n'
import { useVocabularyRebuild } from 'src/composables/useVocabularyRebuild'

const { t, locale } = useI18n()
const { requestVocabularyRebuild } = useVocabularyRebuild()

// --- 组件状态 ---
const leftDrawerOpen = ref(false)
const importDialogVisible = ref(false)

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

function showImportDialog() {
  importDialogVisible.value = true
}

function toggleLanguage() {
  locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  localStorage.setItem('app-locale', locale.value)
}
</script>
