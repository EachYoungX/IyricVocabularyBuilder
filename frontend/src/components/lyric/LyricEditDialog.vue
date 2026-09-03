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

      <q-form class="lyric-dialog-form col column no-wrap" @submit.prevent="saveSong">
        <q-card-section class="editor-body col">
          <div v-if="loading" class="fit flex flex-center">
            <q-spinner color="primary" size="3em" />
          </div>

          <q-banner v-else-if="error" rounded class="bg-red-1 text-negative">
            {{ error }}
          </q-banner>

          <div v-else class="editor-content column no-wrap">
            <section class="song-info-panel">
              <div class="pane-heading">
                <div>
                  <div class="text-subtitle1 text-weight-medium">{{ t('songInformation') }}</div>
                  <div class="text-caption text-grey-7">{{ t('songInformationHint') }}</div>
                </div>
                <SemanticChip v-if="document" tone="count">
                  {{ t('lyricLineCount', { count: document.lines.length }) }}
                </SemanticChip>
              </div>

              <div class="song-info-grid">
                <q-input v-model="editableSong.title" :label="t('title') + ' *'" outlined dense />
                <q-input v-model="editableSong.artist" :label="t('artist') + ' *'" outlined dense />
                <q-input v-model="editableSong.album" :label="t('album')" outlined dense />
              </div>

              <div class="recognized-meta" v-if="songSnapshot?.rawTitle || songSnapshot?.rawArtist">
                <span>{{ t('recognizedSongInfo') }}</span>
                <span v-if="songSnapshot?.rawTitle">{{ t('title') }}: {{ songSnapshot.rawTitle }}</span>
                <span v-if="songSnapshot?.rawArtist">{{ t('artist') }}: {{ songSnapshot.rawArtist }}</span>
              </div>

              <div v-if="document?.credits?.length" class="credit-list">
                <div class="text-caption text-grey-7">{{ t('songCredits') }}</div>
                <div v-for="(credit, creditIndex) in document.credits" :key="`${creditIndex}-${credit.creditType}-${credit.creditValue}`" class="credit-row">
                  <span class="credit-label">{{ credit.creditLabel || credit.creditType }}</span>
                  <span>{{ credit.creditValue }}</span>
                </div>
              </div>
            </section>

            <div class="comparison-grid">
              <section class="lyric-editor-pane raw-pane">
                <div class="pane-heading pane-heading-fixed">
                  <div>
                    <div class="text-subtitle1 text-weight-medium">{{ t('originalImport') }}</div>
                    <div class="text-caption text-grey-7">{{ t('originalImportHint') }}</div>
                  </div>
                </div>
                <pre class="lyrics-preview">{{ rawLyricsForDisplay }}</pre>
              </section>

              <section class="lyric-editor-pane editable-pane">
                <div class="pane-heading pane-heading-fixed">
                  <div>
                    <div class="text-subtitle1 text-weight-medium">{{ t('learningLyrics') }}</div>
                    <div class="text-caption text-grey-7">{{ t('learningLyricsHint') }}</div>
                  </div>
                </div>
                <q-input
                  v-model="editableSong.lyrics"
                  :label="t('lyrics') + ' *'"
                  outlined
                  type="textarea"
                  class="learning-input"
                  input-class="learning-textarea"
                />
              </section>
            </div>
          </div>
        </q-card-section>

        <q-card-actions class="editor-footer justify-end">
          <q-btn
            flat
            color="secondary"
            :label="t('restoreParsedResult')"
            :disable="!document?.normalizedLyrics"
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
        </q-card-actions>
      </q-form>
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
import SemanticChip from 'src/components/SemanticChip.vue';

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
  album: '',
  lyrics: '',
});

const rawLyricsForDisplay = computed(() => document.value?.rawLyrics || t('emptyLyrics'));
const readonlyTitle = computed(() => songSnapshot.value?.rawTitle || songSnapshot.value?.title || props.songTitle || t('untitledSong'));
const readonlyArtist = computed(() => songSnapshot.value?.rawArtist || songSnapshot.value?.artist || t('unknown'));
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
      album: song.album ?? lyricDocument.album ?? '',
      lyrics: song.lyrics ?? lyricDocument.normalizedLyrics ?? lyricDocument.rawLyrics ?? '',
    };
    originalSongJson.value = JSON.stringify(editableSong.value);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : t('loadLyricsForEditingFailed');
  } finally {
    loading.value = false;
  }
}

function restoreOriginalLyrics() {
  if (!document.value?.normalizedLyrics) return;
  editableSong.value = {
    title: readonlyTitle.value,
    artist: readonlyArtist.value,
    album: songSnapshot.value?.album ?? document.value.album ?? '',
    lyrics: document.value.normalizedLyrics,
  };
}

async function saveSong() {
  if (!props.songId || !isFormValid.value) return;
  saving.value = true;
  try {
    const saved = await SongsService.updateSong(props.songId, {
      title: editableSong.value.title.trim(),
      artist: editableSong.value.artist.trim(),
      album: editableSong.value.album.trim() || null,
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
      message: reason instanceof Error ? reason.message : t('saveLyricsFailed'),
    });
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.lyric-dialog {
  width: min(1180px, 96vw);
  height: min(900px, 94dvh);
  max-height: 94dvh;
  background: var(--lv-surface-solid);
}

.lyric-dialog-header {
  flex: 0 0 auto;
  background:
    linear-gradient(90deg, rgba(210, 193, 182, 0.18), transparent),
    var(--lv-surface-solid);
}

.lyric-dialog-form,
.editor-body {
  min-height: 0;
}

.editor-body {
  overflow: hidden;
  padding: 16px 20px;
}

.editor-content {
  height: 100%;
  min-height: 0;
  gap: 16px;
}

.song-info-panel {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
  background: var(--lv-paper);
}

.song-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.recognized-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--lv-muted);
  font-size: 0.82rem;
}

.comparison-grid {
  flex: 1 1 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  min-height: 280px;
}

.lyric-editor-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 16px;
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

.pane-heading-fixed {
  flex: 0 0 auto;
  min-height: 46px;
}

.lyrics-preview {
  flex: 1 1 0;
  min-height: 0;
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

.credit-list {
  max-height: 84px;
  overflow: auto;
  border: 1px solid var(--lv-border);
  border-radius: 10px;
  padding: 10px 12px;
}

.credit-row {
  display: flex;
  gap: 10px;
  padding-top: 5px;
  color: var(--lv-text);
  overflow-wrap: anywhere;
}

.credit-label {
  min-width: 9em;
  color: var(--lv-muted);
}

.learning-input {
  flex: 1 1 0;
  min-height: 0;
}

:deep(.learning-input .q-field__inner),
:deep(.learning-input .q-field__control),
:deep(.learning-input .q-field__control-container),
:deep(.learning-textarea) {
  min-height: 0;
  height: 100%;
}

:deep(.learning-textarea) {
  resize: none;
}

.editor-footer {
  flex: 0 0 auto;
  min-height: 64px;
  padding: 12px 20px;
  border-top: 1px solid var(--lv-line);
  background: var(--lv-surface-solid);
}

@media (max-width: 600px) {
  .lyric-dialog {
    width: 100vw;
    height: 100dvh;
    max-height: 100dvh;
  }

  :deep(.q-card__section) {
    padding-left: 12px;
    padding-right: 12px;
  }

  .editor-body {
    overflow: auto;
  }

  .song-info-grid,
  .comparison-grid {
    grid-template-columns: 1fr;
  }

  .comparison-grid {
    min-height: 520px;
  }

  .lyric-editor-pane {
    min-height: 260px;
  }
}
</style>
