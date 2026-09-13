<template>
  <q-dialog v-model="visible" persistent aria-labelledby="welcome-title">
    <q-card class="welcome-card">
      <q-card-section class="welcome-copy">
        <div class="text-overline">{{ t('welcome.introduction') }}</div>
        <h2 id="welcome-title" class="text-h5 q-mt-sm q-mb-md">{{ t('welcome.title') }}</h2>
        <p>{{ t('welcome.description') }}</p>
        <p>{{ t('welcome.dictionary') }}</p>
        <h3 class="text-subtitle1 text-weight-medium">{{ t('welcome.quickStart') }}</h3>
        <ol>
          <li>{{ t('welcome.import') }}</li>
          <li>{{ t('welcome.analyze') }}</li>
          <li>{{ t('welcome.learn') }}</li>
        </ol>
        <p>
          {{ t('welcome.project') }}
          <a :href="PROJECT_URL" target="_blank" rel="noopener noreferrer"
            @click="openProject">{{ PROJECT_URL }}</a>
        </p>
        <p>{{ t('welcome.license') }}</p>
        <p class="q-mb-none">{{ t('welcome.configure') }}</p>
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md">
        <q-btn no-caps unelevated color="primary" :label="t('welcome.settings')"
          :loading="dismissing" @click="dismiss(true)" />
        <q-btn no-caps flat :label="t('welcome.close')" :disable="dismissing" @click="dismiss(false)" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useQuasar } from 'quasar';
import { WELCOME_DISMISSED_KEY } from 'src/utils/firstLaunch';
import { PROJECT_URL, openProjectPage } from 'src/utils/projectLinks';

const { t } = useI18n();
const router = useRouter();
const $q = useQuasar();
const visible = ref(false);
const dismissing = ref(false);

onMounted(() => { void loadFirstLaunchState(); });

async function loadFirstLaunchState() {
  try {
    const dismissed = window.desktopBridge
      ? await window.desktopBridge.getWelcomeDismissed()
      : window.localStorage.getItem(WELCOME_DISMISSED_KEY) === 'true';
    visible.value = !dismissed;
  } catch {
    visible.value = true;
  }
}

async function dismiss(goToSettings: boolean) {
  dismissing.value = true;
  try {
    if (window.desktopBridge) await window.desktopBridge.dismissWelcome();
    else window.localStorage.setItem(WELCOME_DISMISSED_KEY, 'true');
  } catch {
    $q.notify({ type: 'warning', message: t('welcome.saveFailed') });
  } finally {
    visible.value = false;
    dismissing.value = false;
  }
  if (goToSettings) await router.push({ name: 'Settings' });
}

function openProject(event: MouseEvent) {
  void openProjectPage(event, 'project').catch(() => {
    $q.notify({ type: 'negative', message: t('failed') });
  });
}
</script>

<style scoped>
.q-card.welcome-card {
  width: min(600px, calc(100vw - 32px)) !important;
  max-width: calc(100vw - 32px) !important;
  display: flex;
  flex-direction: column;
}
.welcome-copy { line-height: 1.7; overflow-y: auto; }
.q-card__actions { flex-shrink: 0; }
.welcome-copy ol { padding-left: 1.5em; }
.welcome-copy li + li { margin-top: 6px; }
.welcome-copy a { color: var(--q-primary); overflow-wrap: anywhere; }
</style>
