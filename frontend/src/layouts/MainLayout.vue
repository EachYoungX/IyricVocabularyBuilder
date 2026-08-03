<template>
  <q-layout view="hHh lpR fFf" class="app-shell">
    <q-header class="app-header">
      <q-toolbar class="q-px-md">
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />

        <q-toolbar-title class="row items-center no-wrap q-gutter-sm">
          <span class="brand-mark">Lv</span>
          <span class="brand-title serif-display">{{ t('appTitle') }}</span>
        </q-toolbar-title>
        <q-btn flat dense class="language-toggle" :label="locale === 'zh-CN' ? 'English' : '中文'" @click="toggleLanguage" />
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" show-if-above bordered :width="280" class="app-drawer">
      <div class="drawer-hero q-pa-lg">
        <div class="drawer-kicker">{{ t('appSubtitle') }}</div>
        <div class="text-h5 serif-display drawer-title">
          {{ t('appTitle') }}
        </div>
        <div class="drawer-rule"></div>
      </div>

      <q-list class="q-mt-md">
        <!-- 标题 -->
        <q-item-label header class="text-overline text-grey-6 q-px-lg q-mb-2">
          {{ t('navigation') }}
        </q-item-label>

        <!-- Vocabulary 主页 -->
        <q-item to="/" exact clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders"
          active-class="drawer-active">
          <q-item-section avatar>
            <q-icon name="o_class" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('home') }}</q-item-label>
          </q-item-section>
        </q-item>

        <!-- 歌曲管理 -->
        <q-item to="/songs" clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders"
          active-class="drawer-active">
          <q-item-section avatar>
            <q-icon name="o_library_music" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('songsManager') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item to="/my-vocabulary" clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders"
          active-class="drawer-active">
          <q-item-section avatar>
            <q-icon name="o_menu_book" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('myVocabulary') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item to="/settings" clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders"
          active-class="drawer-active">
          <q-item-section avatar>
            <q-icon name="o_settings" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('settings') }}</q-item-label>
          </q-item-section>
        </q-item>

        <!-- 分隔线 + 功能区（可选） -->
        <q-separator spaced inset class="q-my-lg" />

        <q-item-label header class="text-overline text-grey-6 q-px-lg q-mb-2">
          {{ t('actions') }}
        </q-item-label>

        <q-item to="/song/import" clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders"
          active-class="drawer-active">
          <q-item-section avatar>
            <q-icon name="add" color="accent" size="22px" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ t('importSongs') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable v-ripple class="drawer-nav-item q-mx-md rounded-borders" @click="requestVocabularyRebuild">
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
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useVocabularyRebuild } from 'src/composables/useVocabularyRebuild'

const { t, locale } = useI18n()
const { requestVocabularyRebuild } = useVocabularyRebuild()

// --- 组件状态 ---
const leftDrawerOpen = ref(false)

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

function toggleLanguage() {
  locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  localStorage.setItem('app-locale', locale.value)
}
</script>

<style lang="scss" scoped>
.app-header {
  color: var(--lv-ink);
  background: var(--lv-surface);
  border-bottom: 1px solid var(--lv-line);
  backdrop-filter: blur(18px);
}

.brand-mark {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--lv-paper);
  font-family: var(--lv-font-serif);
  font-size: 17px;
  line-height: 1;
  background: var(--lv-brand-bg);
  border: 1px solid rgba(27, 60, 83, 0.18);
  border-radius: 50%;
  box-shadow: 0 8px 18px rgba(27, 60, 83, 0.12);
}

.brand-title {
  color: var(--lv-ink);
  font-size: 20px;
  font-weight: 700;
}

.language-toggle {
  color: var(--lv-ink-soft);
}

.app-drawer {
  color: var(--lv-ink);
  background: var(--lv-page-bg);
  border-right-color: var(--lv-line);
}

.drawer-hero {
  border-bottom: 1px solid var(--lv-line);
}

.drawer-kicker {
  color: var(--lv-ink-soft);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.drawer-title {
  color: var(--lv-ink);
  margin-top: 6px;
  line-height: 1.05;
}

.drawer-rule {
  width: 42px;
  height: 2px;
  margin-top: 16px;
  background: linear-gradient(90deg, var(--lv-blue), var(--lv-sand));
  border-radius: 999px;
}

:deep(.q-item) {
  color: var(--lv-ink-soft);
}

.drawer-nav-item {
  margin-bottom: 8px;
}

:deep(.drawer-active) {
  color: var(--lv-drawer-active-ink) !important;
  background: var(--lv-drawer-active-bg) !important;
}

:deep(.drawer-active .q-icon) {
  color: var(--lv-drawer-active-ink) !important;
}

:deep(.q-item__label--header) {
  color: var(--lv-muted);
  letter-spacing: 0.12em;
}
</style>
