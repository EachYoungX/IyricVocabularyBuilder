<template>
  <q-page class="song-import-page">
    <div class="song-import-shell">
      <header class="import-header">
        <div>
          <div class="text-overline text-primary">{{ t('songImportWorkspace') }}</div>
          <h1 class="serif-display">{{ t('songImportTitle') }}</h1>
          <p>{{ t('songImportBoundaryHint') }}</p>
        </div>
        <q-btn flat no-caps icon="library_music" :label="t('songsManager')" to="/songs" />
      </header>

      <section class="source-band">
        <q-file
          v-model="selectedFiles"
          outlined
          multiple
          accept=".txt,.json,.lrc,.srt,.qrc"
          :label="t('dragDropFiles')"
          @update:model-value="handleFileSelect"
        >
          <template #prepend>
            <q-icon name="cloud_upload" />
          </template>
        </q-file>

        <q-btn outline no-caps icon="playlist_add" :label="t('addSongManually')" @click="addManualDraft" />
      </section>

      <section class="import-workspace">
        <aside class="draft-list">
          <div class="row items-center justify-between q-mb-sm">
            <div class="text-subtitle2">{{ t('previewAndEdit') }} ({{ drafts.length }})</div>
            <q-btn v-if="drafts.length" flat dense no-caps color="negative" :label="t('clearAll')" @click="clearDrafts" />
          </div>

          <q-list v-if="drafts.length" separator>
            <q-item
              v-for="(draft, index) in drafts"
              :key="`${draft.sourceName || 'manual'}-${index}`"
              clickable
              :active="index === activeIndex"
              active-class="active-draft"
              @click="activeIndex = index"
            >
              <q-item-section avatar>
                <q-avatar color="primary" text-color="white" icon="music_note" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ draft.title || t('untitledSong') }}</q-item-label>
                <q-item-label caption>{{ draft.artist || t('unknownArtist') }}</q-item-label>
                <div class="row q-gutter-xs q-mt-xs">
                  <q-chip v-if="draft.sourceFormat" dense size="sm" color="grey-2">{{ draft.sourceFormat }}</q-chip>
                  <q-chip v-if="draft.importSummary" dense size="sm" color="primary" text-color="white">
                    {{ t('recognizedLyricLines', { count: draft.importSummary.lyricLines }) }}
                  </q-chip>
                </div>
              </q-item-section>
              <q-item-section side>
                <q-btn flat round dense icon="delete" color="negative" @click.stop="removeDraft(index)">
                  <q-tooltip>{{ t('removeThisSong') }}</q-tooltip>
                </q-btn>
              </q-item-section>
            </q-item>
          </q-list>

          <div v-else class="empty-drafts">
            <q-icon name="library_music" size="48px" color="grey-5" />
            <div>{{ t('noSongsReadyForImport') }}</div>
          </div>
        </aside>

        <main v-if="activeDraft" class="editor-grid">
          <section class="raw-panel">
            <div class="panel-title">
              <div>
                <div class="text-subtitle2">{{ t('rawImportContent') }}</div>
                <div class="text-caption text-grey-7">{{ activeDraft.sourceName || t('manualInputSource') }}</div>
              </div>
              <q-chip v-if="activeDraft.sourceFormat" dense color="grey-2">{{ activeDraft.sourceFormat }}</q-chip>
            </div>
            <q-input
              :model-value="activeDraft.rawSourceContent || t('manualInputSource')"
              readonly
              outlined
              type="textarea"
              input-class="raw-source-text"
            />
          </section>

          <section class="edit-panel">
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <q-input v-model="activeDraft.title" outlined :label="t('title')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model="activeDraft.artist" outlined :label="t('artist')" />
              </div>
            </div>

            <q-input
              v-model="activeDraft.lyrics"
              outlined
              type="textarea"
              class="lyrics-editor q-mt-md"
              :label="t('editableLyricsContent')"
            />

            <q-banner rounded class="import-boundary-banner q-mt-md">
              {{ t('songImportBoundaryHint') }}
            </q-banner>
          </section>
        </main>

        <main v-else class="no-active-draft">
          <q-icon name="upload_file" size="64px" color="grey-5" />
          <div>{{ t('chooseFiles') }}</div>
        </main>
      </section>
    </div>

    <div class="import-action-bar">
      <div class="text-caption">
        <span v-if="drafts.length">{{ t('songsReady', { count: drafts.length }) }}</span>
        <span v-if="drafts.length > importLimit" class="text-warning q-ml-sm">
          {{ t('importLimitExceeded', { max: importLimit }) }}
        </span>
      </div>
      <q-space />
      <q-btn flat no-caps :label="t('cancel')" to="/songs" />
      <q-btn
        outline
        no-caps
        :disable="!activeDraft?.parsedLyricsContent"
        :label="t('restoreParsedLyrics')"
        @click="restoreParsedLyrics"
      />
      <q-btn
        color="primary"
        no-caps
        icon="publish"
        :label="t('importSongsButton', { count: drafts.length })"
        :loading="isImporting"
        :disable="!canImport"
        @click="importSongs"
      />
    </div>

    <q-dialog v-model="showProgressDialog" persistent>
      <q-card class="import-progress-card">
        <q-card-section>
          <div class="text-h6">{{ t('importing') }}</div>
        </q-card-section>
        <q-card-section>
          <q-linear-progress :value="importProgress" color="primary" size="10px" rounded class="q-mb-sm" />
          <div class="row justify-between text-caption">
            <span>{{ t('importSuccess') }}: {{ importTask?.successCount || 0 }}</span>
            <span class="text-negative" v-if="importTask?.failedCount">{{ t('failures') }}: {{ importTask.failedCount }}</span>
          </div>
        </q-card-section>
        <q-card-actions align="right" v-if="!isImporting">
          <q-btn color="primary" no-caps :label="t('done')" @click="finishImport" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Notify } from 'quasar';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ImportTaskResult as ImportTaskResultEnum } from 'src/services/api/models/ImportTaskResult';
import type { ImportTaskResult, SongImportRequest } from 'src/services/api';
import { useSongsStore } from 'src/stores/songsStore';
import type { ExtendedSongImportRequest } from 'src/types/songImport';
import { buildImportSummary, parseImportFileContent } from 'src/utils/lyricsImportParser';
import { detectLyricsLanguage } from 'src/utils/languageDetector';
import { loadAppSettings } from 'src/utils/appSettings';

const importLimit = 100;
const { t } = useI18n();
const router = useRouter();
const songsStore = useSongsStore();

const selectedFiles = ref<File[] | null>(null);
const drafts = ref<ExtendedSongImportRequest[]>([]);
const activeIndex = ref(0);
const isImporting = ref(false);
const importTask = ref<ImportTaskResult | null>(null);
const taskId = ref<string | null>(null);
const showProgressDialog = ref(false);

const activeDraft = computed(() => drafts.value[activeIndex.value] || null);
const canImport = computed(() => {
  return drafts.value.length > 0
    && drafts.value.length <= importLimit
    && drafts.value.every((draft) => draft.title?.trim() && draft.artist?.trim() && draft.lyrics?.trim());
});
const importProgress = computed(() => {
  if (!importTask.value) return 0;
  const processed = (importTask.value.successCount ?? 0) + (importTask.value.failedCount ?? 0);
  return importTask.value.total ? processed / importTask.value.total : 0;
});

async function handleFileSelect(files: File[] | null) {
  if (!files?.length) {
    selectedFiles.value = null;
    return;
  }

  Notify.create({
    group: 'file-parsing',
    spinner: true,
    message: t('parsingFiles', { count: files.length }),
    timeout: 0,
    type: 'ongoing',
    position: 'top-right',
  });

  const parsedDrafts: ExtendedSongImportRequest[] = [];
  let failedCount = 0;
  for (const file of files) {
    try {
      const content = await file.text();
      parsedDrafts.push(...parseImportFileContent(file.name, content, t, loadAppSettings()).map(attachLanguageDetection));
    } catch (error) {
      failedCount += 1;
      console.warn(`Failed to parse ${file.name}`, error);
    }
  }

  drafts.value = [...drafts.value, ...parsedDrafts];
  if (drafts.value.length && !activeDraft.value) activeIndex.value = 0;
  selectedFiles.value = null;

  Notify.create({
    group: 'file-parsing',
    type: failedCount ? 'warning' : 'positive',
    message: t('filesParsedSuccessfully', { songCount: parsedDrafts.length, fileCount: files.length }),
    timeout: 4000,
    position: 'top-right',
  });
}

function addManualDraft() {
  const draft: ExtendedSongImportRequest = {
    title: '',
    artist: '',
    lyrics: '',
    rawSourceContent: '',
    parsedLyricsContent: '',
    sourceName: t('manualInputSource'),
    sourceFormat: 'MANUAL',
    importSummary: buildImportSummary(''),
  };
  drafts.value.push(draft);
  activeIndex.value = drafts.value.length - 1;
}

function removeDraft(index: number) {
  drafts.value.splice(index, 1);
  if (activeIndex.value >= drafts.value.length) activeIndex.value = Math.max(0, drafts.value.length - 1);
}

function clearDrafts() {
  drafts.value = [];
  activeIndex.value = 0;
}

function restoreParsedLyrics() {
  if (!activeDraft.value?.parsedLyricsContent) return;
  activeDraft.value.lyrics = activeDraft.value.parsedLyricsContent;
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

async function importSongs() {
  if (!canImport.value) return;

  isImporting.value = true;
  showProgressDialog.value = true;
  importTask.value = null;

  const basicSongs: SongImportRequest[] = drafts.value.map((draft) => ({
    title: draft.title,
    artist: draft.artist,
    lyrics: draft.lyrics,
  }));

  try {
    const result: unknown = await songsStore.importSongs(basicSongs);
    const receivedTaskId = readTaskId(result);
    if (!receivedTaskId) throw new Error('No Task ID received from server');

    taskId.value = receivedTaskId;
    importTask.value = {
      taskId: receivedTaskId,
      status: ImportTaskResultEnum.status.PENDING,
      total: drafts.value.length,
      successCount: 0,
      failedCount: 0,
      failedItems: [],
    };
    pollTaskStatus(receivedTaskId);
  } catch (error) {
    isImporting.value = false;
    Notify.create({
      type: 'negative',
      message: error instanceof Error ? error.message : t('importFailedMessage'),
      position: 'top-right',
    });
  }
}

function readTaskId(result: unknown) {
  if (typeof result === 'string') return result;
  if (!result || typeof result !== 'object') return null;
  const record = result as Record<string, unknown>;
  return (record.taskId as string) || (record.id as string) || (record.task_id as string) || null;
}

function pollTaskStatus(currentTaskId: string) {
  const pollInterval = 1500;
  let attempts = 0;
  const maxAttempts = 120;

  const poll = async () => {
    if (taskId.value !== currentTaskId) return;
    attempts += 1;
    try {
      const taskResult = await songsStore.checkImportTaskStatus(currentTaskId);
      if (taskResult) {
        importTask.value = taskResult;
        if (taskResult.status === ImportTaskResultEnum.status.COMPLETED || taskResult.status === ImportTaskResultEnum.status.FAILED) {
          isImporting.value = false;
          if (taskResult.status === ImportTaskResultEnum.status.COMPLETED) {
            await songsStore.fetchAllSongs(false);
          }
          return;
        }
      }
      if (attempts < maxAttempts) window.setTimeout(() => void poll(), pollInterval);
      else {
        isImporting.value = false;
        Notify.create({ type: 'warning', message: t('taskTimedOut'), position: 'top-right' });
      }
    } catch {
      isImporting.value = false;
      Notify.create({ type: 'negative', message: t('importFailedMessage'), position: 'top-right' });
    }
  };

  window.setTimeout(() => void poll(), pollInterval);
}

async function finishImport() {
  showProgressDialog.value = false;
  if (importTask.value?.status === ImportTaskResultEnum.status.COMPLETED && !importTask.value.failedCount) {
    clearDrafts();
    await router.push('/songs');
  }
}
</script>

<style lang="scss" scoped>
.song-import-page {
  min-height: 100%;
  padding: 32px 32px 96px;
  color: var(--lv-ink);
  background: var(--lv-page-bg);
}

.song-import-shell {
  max-width: 1440px;
  margin: 0 auto;
}

.import-header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;

  h1 {
    margin: 0;
    font-size: 32px;
    line-height: 1.2;
  }

  p {
    max-width: 760px;
    margin: 8px 0 0;
    color: var(--lv-ink-soft);
  }
}

.source-band {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  background: var(--lv-surface);
  border: 1px solid var(--lv-line);
  border-radius: 8px;
}

.import-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.draft-list,
.raw-panel,
.edit-panel,
.no-active-draft {
  background: var(--lv-surface);
  border: 1px solid var(--lv-line);
  border-radius: 8px;
}

.draft-list {
  min-height: 560px;
  max-height: calc(100vh - 260px);
  padding: 14px;
  overflow: auto;
}

.active-draft {
  color: var(--lv-ink);
  background: rgba(27, 60, 83, 0.08);
}

.empty-drafts,
.no-active-draft {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: var(--lv-ink-soft);
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.42fr) minmax(360px, 0.58fr);
  gap: 18px;
}

.raw-panel,
.edit-panel {
  min-height: 560px;
  padding: 16px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.raw-source-text {
  min-height: 420px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  line-height: 1.55;
}

.lyrics-editor :deep(textarea) {
  min-height: 410px;
  line-height: 1.62;
}

.import-boundary-banner {
  color: var(--lv-ink);
  background: rgba(212, 167, 98, 0.14);
  border: 1px solid rgba(212, 167, 98, 0.32);
}

.import-action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1000;
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 32px;
  color: var(--lv-ink);
  background: var(--lv-surface);
  border-top: 1px solid var(--lv-line);
  box-shadow: 0 -10px 24px rgba(27, 60, 83, 0.08);
}

.import-progress-card {
  width: min(420px, calc(100vw - 32px));
}

@media (max-width: 900px) {
  .song-import-page {
    padding: 20px 16px 112px;
  }

  .import-header,
  .source-band,
  .import-workspace,
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .import-header {
    display: grid;
  }

  .draft-list,
  .raw-panel,
  .edit-panel {
    min-height: auto;
    max-height: none;
  }

  .raw-panel {
    order: 2;
  }

  .edit-panel {
    order: 1;
  }

  .import-action-bar {
    flex-wrap: wrap;
    padding: 10px 16px;
  }
}
</style>
