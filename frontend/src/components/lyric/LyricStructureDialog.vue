<template>
  <q-dialog :model-value="modelValue" maximized-on-mobile @update:model-value="updateDialogVisibility">
    <q-card class="lyric-dialog column no-wrap">
      <q-card-section class="row items-center q-pb-sm lyric-dialog-header">
        <div>
          <div class="text-h6 serif-display">{{ t('structuredLyrics') }}</div>
          <div class="text-caption text-grey-7">{{ songTitle }}</div>
        </div>
        <q-space />
        <q-btn v-close-popup flat round dense icon="close" :aria-label="t('close')" />
      </q-card-section>

      <q-separator />

      <q-card-section v-if="document" class="q-py-sm">
        <div class="row q-gutter-sm items-center">
          <q-chip dense color="primary" text-color="white">
            {{ t('lyricLineCount', { count: document.lines.length }) }}
          </q-chip>
          <q-chip dense>{{ t('importVersion', { version: document.importVersion }) }}</q-chip>
          <q-chip dense color="accent" text-color="primary">
            {{ t('hiddenLineCount', { count: hiddenCount }) }}
          </q-chip>
          <q-chip dense color="secondary" text-color="white">
            {{ t('overrideCount', { count: overrideCount }) }}
          </q-chip>
        </div>

        <q-expansion-item dense icon="difference" :label="t('compareRawAndNormalized')" class="q-mt-sm">
          <div class="row q-col-gutter-md q-pa-sm">
            <div class="col-12 col-md-6">
              <div class="text-caption text-weight-medium q-mb-xs">{{ t('rawLyrics') }}</div>
              <pre class="lyrics-preview">{{ document.rawLyrics }}</pre>
            </div>
            <div class="col-12 col-md-6">
              <div class="text-caption text-weight-medium q-mb-xs">{{ t('normalizedLyrics') }}</div>
              <pre class="lyrics-preview">{{ document.normalizedLyrics }}</pre>
            </div>
          </div>
        </q-expansion-item>
      </q-card-section>

      <q-separator />

      <q-card-section class="col scroll q-pa-sm">
        <div v-if="loading" class="fit flex flex-center">
          <q-spinner color="primary" size="3em" />
        </div>
        <q-banner v-else-if="error" rounded class="bg-red-1 text-negative">
          {{ error }}
        </q-banner>
        <q-list v-else separator bordered class="rounded-borders">
          <q-item v-for="line in editableLines" :key="line.id" class="column q-pa-sm">
            <div class="row items-center q-gutter-sm full-width">
              <div class="text-caption text-grey-6 line-index">#{{ line.lineIndex + 1 }}</div>
              <q-select
                v-model="line.lineType"
                :options="lineTypeOptions"
                emit-value
                map-options
                dense
                outlined
                class="line-type"
              />
              <q-toggle v-model="line.hidden" :label="t('hidden')" dense />
              <q-chip v-if="line.userOverride" dense color="secondary" text-color="white">
                {{ t('userOverride') }}
              </q-chip>
              <q-space />
              <span class="text-caption text-grey-6">
                {{ t('confidence', { value: Math.round(line.confidence * 100) }) }}
              </span>
              <q-btn
                color="primary"
                dense
                flat
                icon="save"
                :label="t('saveLine')"
                :loading="savingLineId === line.id"
                @click="saveLine(line)"
              />
            </div>
            <div class="text-caption text-grey-7 q-mt-xs original-line">{{ line.originalText || t('emptyLine') }}</div>
            <q-input v-model="line.normalizedText" dense outlined autogrow class="q-mt-xs" />
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Notify } from 'quasar';
import { useI18n } from 'vue-i18n';
import {
  LyricsService,
  LyricLineType,
  type LyricDocument,
  type LyricLine,
} from 'src/services/api';

const props = defineProps<{
  modelValue: boolean;
  songId?: number;
  songTitle?: string;
}>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();
const { t } = useI18n();

const document = ref<LyricDocument | null>(null);
const editableLines = ref<LyricLine[]>([]);
const loading = ref(false);
const error = ref('');
const savingLineId = ref<number | null>(null);

const hiddenCount = computed(() => editableLines.value.filter((line) => line.hidden).length);
const overrideCount = computed(() => editableLines.value.filter((line) => line.userOverride).length);
const lineTypeOptions = computed(() => Object.values(LyricLineType).map((value) => ({
  value,
  label: t(`lyricLineTypes.${value}`),
})));

watch(
  () => [props.modelValue, props.songId] as const,
  ([visible, songId]) => {
    if (visible && songId) void loadDocument(songId);
  },
  { immediate: true },
);

function updateDialogVisibility(value: boolean) {
  emit('update:modelValue', value);
}

async function loadDocument(songId: number) {
  loading.value = true;
  error.value = '';
  try {
    document.value = await LyricsService.getStructuredLyrics(songId);
    editableLines.value = document.value.lines.map((line) => ({ ...line }));
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : t('loadStructuredLyricsFailed');
  } finally {
    loading.value = false;
  }
}

async function saveLine(line: LyricLine) {
  if (!props.songId) return;
  savingLineId.value = line.id;
  try {
    const saved = await LyricsService.updateLyricLine(props.songId, line.id, {
      normalizedText: line.normalizedText,
      lineType: line.lineType,
      hidden: line.hidden,
    });
    Object.assign(line, saved);
    Notify.create({ type: 'positive', message: t('lyricLineSaved') });
  } catch (reason) {
    Notify.create({
      type: 'negative',
      message: reason instanceof Error ? reason.message : t('saveLyricLineFailed'),
    });
  } finally {
    savingLineId.value = null;
  }
}
</script>

<style scoped lang="scss">
.lyric-dialog {
  width: min(1200px, 96vw);
  height: min(900px, 92vh);
  background: var(--lv-surface-solid);
}

.lyric-dialog-header {
  background:
    linear-gradient(90deg, rgba(210, 193, 182, 0.18), transparent),
    var(--lv-surface-solid);
}

.lyrics-preview {
  margin: 0;
  padding: 12px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
  background: var(--lv-paper);
  color: var(--lv-ink);
  font-family: var(--lv-font-serif);
  line-height: 1.7;
}

.line-index {
  width: 36px;
}

.line-type {
  min-width: 190px;
}

.original-line {
  color: var(--lv-muted);
  overflow-wrap: anywhere;
}

:deep(.q-item) {
  margin: 6px;
  border: 1px solid transparent;
  border-radius: var(--lv-radius-sm);
}

:deep(.q-item:hover) {
  border-color: var(--lv-line);
  background: rgba(249, 243, 239, 0.62);
}

@media (max-width: 600px) {
  .lyric-dialog {
    width: 100vw;
    height: 100dvh;
  }

  :deep(.q-card__section) {
    padding-left: 12px;
    padding-right: 12px;
  }

  :deep(.q-item > .row) {
    align-items: flex-start;
  }

  .line-type {
    min-width: 100%;
    order: 4;
  }

  .line-index {
    width: auto;
  }
}
</style>
