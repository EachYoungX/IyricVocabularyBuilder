<template>
  <q-page padding class="dictionary-source-page">
    <div class="q-mx-auto page-content">
      <q-card flat bordered class="source-card">
        <q-card-section>
          <div class="text-h5 text-weight-bold serif-display source-title">{{ t('dictionarySourceTitle') }}</div>
          <div class="text-body2 text-grey-7 q-mt-sm">{{ t('dictionarySourceSubtitle') }}</div>
        </q-card-section>

        <q-separator />

        <q-card-section v-if="loading" class="text-center q-py-xl">
          <q-spinner color="primary" size="32px" />
        </q-card-section>

        <q-card-section v-else-if="sources.length" class="q-gutter-md">
          <div class="source-grid">
            <q-card v-for="item in sources" :key="item.sourceName" flat bordered class="source-item">
              <q-card-section>
                <div class="text-h6 text-weight-bold source-item-title">{{ item.sourceName }}</div>
                <div class="source-detail">
                  <span class="source-label">{{ t('dictionarySourceUrl') }}</span>
                  <a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer" class="source-link">
                    {{ item.sourceUrl }}
                  </a>
                </div>
                <div class="source-detail">
                  <span class="source-label">{{ t('dictionaryLicenseName') }}</span>
                  <strong>{{ item.licenseName }}</strong>
                </div>
                <div class="source-detail">
                  <span class="source-label">{{ t('dictionaryAttribution') }}</span>
                  <span>{{ item.attributionText }}</span>
                </div>
              </q-card-section>
              <q-separator />
              <q-card-section class="source-flags">
                <div><q-icon :name="item.requiresAttribution ? 'check_circle' : 'cancel'" color="primary" /> {{ t('requiresAttribution') }}: {{ yesNo(item.requiresAttribution) }}</div>
                <div><q-icon :name="item.commercialUseAllowed ? 'check_circle' : 'cancel'" color="primary" /> {{ t('commercialUseAllowed') }}: {{ yesNo(item.commercialUseAllowed) }}</div>
                <div><q-icon :name="item.redistributionAllowed ? 'check_circle' : 'cancel'" color="primary" /> {{ t('redistributionAllowed') }}: {{ yesNo(item.redistributionAllowed) }}</div>
              </q-card-section>
            </q-card>
          </div>

          <q-banner rounded class="source-note">
            {{ t('dictionarySourceNote') }}
          </q-banner>
        </q-card-section>

        <q-card-section v-else class="text-negative">
          {{ t('dictionarySourceLoadFailed') }}
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { DictionaryService, type DictionarySource } from 'src/services/api';

const { t } = useI18n();
const loading = ref(false);
const sources = ref<DictionarySource[]>([]);

function yesNo(value?: boolean) {
  if (value === undefined) return t('unknown');
  return value ? t('yes') : t('no');
}

async function loadDictionarySource() {
  loading.value = true;
  try {
    sources.value = await DictionaryService.getDictionarySource();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadDictionarySource();
});
</script>

<style scoped>
.page-content {
  max-width: 1120px;
}

.source-card {
  background: var(--lv-surface-solid);
  box-shadow: var(--lv-shadow-soft);
}

.source-title {
  color: var(--lv-ink);
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.source-item {
  height: 100%;
  background: var(--lv-paper);
  border-color: var(--lv-line);
}

.source-item-title {
  color: var(--lv-ink);
  margin-bottom: 16px;
}

.source-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 14px;
  color: var(--lv-ink-soft);
}

.source-label {
  color: var(--lv-muted);
  font-size: 0.78rem;
}

.source-detail strong {
  color: var(--lv-ink);
}

.source-link {
  color: var(--lv-blue);
  overflow-wrap: anywhere;
}

.source-flags {
  display: grid;
  gap: 8px;
  color: var(--lv-ink-soft);
  font-size: 0.84rem;
}

.source-banner {
  color: var(--lv-ink-soft);
  background: var(--lv-accent-soft);
}

.source-note {
  color: var(--lv-ink);
  background: rgba(210, 193, 182, 0.22);
  border: 1px solid var(--lv-line);
}

@media (max-width: 700px) {
  .source-grid {
    grid-template-columns: 1fr;
  }
}
</style>
