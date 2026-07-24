<template>
  <q-dialog :model-value="modelValue" maximized-on-mobile @update:model-value="updateDialogVisibility">
    <q-card class="lyric-dialog column no-wrap">
      <q-card-section class="row items-center q-pb-sm lyric-dialog-header">
        <div>
          <div class="text-h6 serif-display">{{ t('editLyrics') }}</div>
          <div class="text-caption text-grey-7">{{ songTitle || t('untitledSong') }}</div>
        </div>
        <q-space />
        <q-btn v-close-popup flat round dense icon="close" :aria-label="t('close')" />
      </q-card-section>

      <q-separator />

      <q-card-section class="col scroll">
        <div v-if="loading" class="fit flex flex-center">
          <q-spinner color="primary" size="3em" />
        </div>

        <q-banner v-else-if="error" rounded class="bg-red-1 text-negative">
          {{ error }}
        </q-banner>

        <div v-else class="row q-col-gutter-lg lyric-editor-grid">
          <div class="col-12 col-md-6">
            <section class="lyric-editor-pane raw-pane">
              <div class="pane-heading">
                <div>
                  <div class="text-subtitle1 text-weight-medium">{{ t('originalImport') }}</div>
                  <div class="text-caption text-grey-7">{{ t('originalImportHint') }}</div>
                </div>
                <q-chip v-if="document" dense color="primary" text-color="white">
                  {{ t('lyricLineCount', { count: document.lines.length }) }}
                </q-chip>
              </div>

              <div class="raw-meta-grid">
                <div>
                  <div class="text-caption text-grey-7">{{ t('title') }}</div>
                  <div class="text-body1">{{ readonlyTitle }}</div>
                </div>
                <div>
                  <div class="text-caption text-grey-7">{{ t('artist') }}</div>
                  <div class="text-body1">{{ readonlyArtist }}</div>
                </div>
              </div>

              <pre class="lyrics-preview">{{ rawLyricsForDisplay }}</pre>
            </section>
          </div>

          <div class="col-12 col-md-6">
            <q-form class="lyric-editor-pane editable-pane" @submit.prevent="saveSong">
              <div class="pane-heading">
                <div>
                  <div class="text-subtitle1 text-weight-medium">{{ t('learningLyrics') }}</div>
                  <div class="text-caption text-grey-7">{{ t('learningLyricsHint') }}</div>
                </div>
              </div>

              <q-input v-model="editableSong.title" :label="t('title') + ' *'" outlined />
              <q-input v-model="editableSong.artist" :label="t('artist') + ' *'" outlined />
              <q-input
                v-model="editableSong.lyrics"
                :label="t('lyrics') + ' *'"
                outlined
                type="textarea"
                autogrow
                class="lyrics-input"
              />

              <div class="row q-gutter-sm justify-end">
                <q-btn
                  flat
                  color="secondary"
                  :label="t('restoreOriginalLyrics')"
                  :disable="!document?.rawLyrics"
                  @click="restoreOriginalLyrics"
                />
                <q-btn flat :label="t('cancel')" v-close-popup />
                <q-btn
                  color="primary"
                  type="submit"
                  :label="t('save')"
                  :loading="saving"
                  :disable="!isFormValid || !isDirty"
                />
              </div>
            </q-form>
          </div>
        </div>
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
  SongsService,
  type LyricDocument,
  type Song,
} from 'src/services/api';

const props = defineProps<{
  modelValue: boolean;
  songId?: number;
  songTitle?: string;
}>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  saved: [song: Song];
}>();
const { t } = useI18n();

const document = ref<LyricDocument | null>(null);
const songSnapshot = ref<Song | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const originalSongJson = ref('');
const editableSong = ref({
  title: '',
  artist: '',
  lyrics: '',
});

const rawLyricsForDisplay = computed(() => document.value?.rawLyrics || t('emptyLyrics'));
const readonlyTitle = computed(() => songSnapshot.value?.title || props.songTitle || t('untitledSong'));
const readonlyArtist = computed(() => songSnapshot.value?.artist || t('unknown'));
const isDirty = computed(() => JSON.stringify(editableSong.value) !== originalSongJson.value);
const isFormValid = computed(() =>
  editableSong.value.title.trim().length > 0
  && editableSong.value.artist.trim().length > 0
  && editableSong.value.lyrics.trim().length > 0,
);

watch(
  () => [props.modelValue, props.songId] as const,
  ([visible, songId]) => {
    if (visible && songId) void loadEditorData(songId);
  },
  { immediate: true },
);

function updateDialogVisibility(value: boolean) {
  emit('update:modelValue', value);
}

async function loadEditorData(songId: number) {
  loading.value = true;
  error.value = '';
  try {
    const [lyricDocument, song] = await Promise.all([
      LyricsService.getStructuredLyrics(songId),
      SongsService.getSongById(songId),
    ]);
    document.value = lyricDocument;
    songSnapshot.value = song;
    editableSong.value = {
      title: song.title ?? '',
      artist: song.artist ?? '',
      lyrics: song.lyrics ?? lyricDocument.normalizedLyrics ?? lyricDocument.rawLyrics ?? '',
    };
    originalSongJson.value = JSON.stringify(editableSong.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : t('loadStructuredLyricsFailed');
  } finally {
    loading.value = false;
  }
}

function restoreOriginalLyrics() {
  if (!document.value?.rawLyrics) return;
  editableSong.value = {
    title: readonlyTitle.value,
    artist: readonlyArtist.value,
    lyrics: document.value.rawLyrics,
  };
}

async function saveSong() {
  if (!props.songId || !isFormValid.value) return;
  saving.value = true;
  try {
    const saved = await SongsService.updateSong(props.songId, {
      title: editableSong.value.title.trim(),
      artist: editableSong.value.artist.trim(),
      lyrics: editableSong.value.lyrics,
    });
    originalSongJson.value = JSON.stringify(editableSong.value);
    Notify.create({ type: 'positive', position: 'top-right', message: t('songUpdatedSuccessfullyMessage', { title: saved.title }) });
    emit('saved', saved);
    emit('update:modelValue', false);
  } catch (reason) {
    Notify.create({
      type: 'negative',
      position: 'top-right',
      message: reason instanceof Error ? reason.message : t('saveLyricLineFailed'),
    });
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.lyric-dialog {
  width: min(1180px, 96vw);
  height: min(820px, 92vh);
  background: var(--lv-surface-solid);
}

.lyric-dialog-header {
  background:
    linear-gradient(90deg, rgba(210, 193, 182, 0.18), transparent),
    var(--lv-surface-solid);
}

.lyric-editor-grid {
  min-height: 100%;
}

.lyric-editor-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  padding: 18px;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
  background: var(--lv-paper);
}

.pane-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.raw-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
  background: var(--lv-surface-solid);
}

.lyrics-preview {
  flex: 1;
  min-height: 360px;
  margin: 0;
  padding: 14px;
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

.lyrics-input {
  flex: 1;
  min-height: 360px;
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
  .lyric-editor-pane,
  .lyrics-preview,
  .lyrics-input {
    min-height: 260px;
  }
}
</style>
