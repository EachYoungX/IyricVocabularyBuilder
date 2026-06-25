<template>
  <q-page padding class="dictionary-source-page">
    <div class="q-mx-auto page-content">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 text-weight-bold">{{ t('dictionarySourceTitle') }}</div>
          <div class="text-body2 text-grey-7 q-mt-sm">{{ t('dictionarySourceSubtitle') }}</div>
        </q-card-section>

        <q-separator />

        <q-card-section v-if="loading" class="text-center q-py-xl">
          <q-spinner color="primary" size="32px" />
        </q-card-section>

        <q-card-section v-else-if="source" class="q-gutter-md">
          <q-list bordered separator>
            <q-item>
              <q-item-section>
                <q-item-label caption>{{ t('dictionarySourceName') }}</q-item-label>
                <q-item-label>{{ source.sourceName }}</q-item-label>
              </q-item-section>
            </q-item>

            <q-item v-if="source.sourceUrl" clickable tag="a" :href="source.sourceUrl" target="_blank">
              <q-item-section>
                <q-item-label caption>{{ t('dictionarySourceUrl') }}</q-item-label>
                <q-item-label class="text-primary">{{ source.sourceUrl }}</q-item-label>
              </q-item-section>
            </q-item>

            <q-item>
              <q-item-section>
                <q-item-label caption>{{ t('dictionaryLicenseName') }}</q-item-label>
                <q-item-label>{{ source.licenseName }}</q-item-label>
              </q-item-section>
            </q-item>

            <q-item>
              <q-item-section>
                <q-item-label caption>{{ t('dictionaryAttribution') }}</q-item-label>
                <q-item-label>{{ source.attributionText }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>

          <div class="row q-col-gutter-sm">
            <div class="col-12 col-sm-4">
              <q-banner rounded class="bg-grey-2">
                <q-icon :name="source.requiresAttribution ? 'check_circle' : 'cancel'" color="primary" class="q-mr-sm" />
                {{ t('requiresAttribution') }}: {{ yesNo(source.requiresAttribution) }}
              </q-banner>
            </div>
            <div class="col-12 col-sm-4">
              <q-banner rounded class="bg-grey-2">
                <q-icon :name="source.commercialUseAllowed ? 'check_circle' : 'cancel'" color="primary" class="q-mr-sm" />
                {{ t('commercialUseAllowed') }}: {{ yesNo(source.commercialUseAllowed) }}
              </q-banner>
            </div>
            <div class="col-12 col-sm-4">
              <q-banner rounded class="bg-grey-2">
                <q-icon :name="source.redistributionAllowed ? 'check_circle' : 'cancel'" color="primary" class="q-mr-sm" />
                {{ t('redistributionAllowed') }}: {{ yesNo(source.redistributionAllowed) }}
              </q-banner>
            </div>
          </div>

          <q-banner rounded class="bg-blue-1 text-blue-10">
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
const source = ref<DictionarySource | null>(null);

function yesNo(value?: boolean) {
  if (value === undefined) return t('unknown');
  return value ? t('yes') : t('no');
}

async function loadDictionarySource() {
  loading.value = true;
  try {
    source.value = await DictionaryService.getDictionarySource();
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
  max-width: 880px;
}
</style>
