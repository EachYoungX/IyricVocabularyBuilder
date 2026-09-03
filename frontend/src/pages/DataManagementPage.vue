<template>
  <q-page padding class="data-management-page">
    <div class="q-mx-auto data-management-content">
      <section class="page-masthead page-intro">
        <div class="page-kicker">{{ t('settingsPage.dataKicker') }}</div>
        <h1 class="serif-display page-title">{{ t('settingsPage.dataTitle') }}</h1>
        <p class="page-caption">{{ t('settingsPage.dataCaption') }}</p>
      </section>

      <div class="management-sections">
        <section class="management-section">
          <SettingsSectionHeading icon="o_insights" :title="t('settingsPage.dataOverviewTitle')"
            :caption="t('settingsPage.dataOverviewCaption')" />
          <div class="stat-grid q-mt-md">
            <div v-for="item in dataStats" :key="item.label" class="stat-card">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </section>

        <section class="management-section">
          <SettingsSectionHeading icon="o_backup" :title="t('settingsPage.backupRestoreTitle')"
            :caption="t('settingsPage.backupRestoreCaption')" />
          <div class="backup-layout q-mt-md">
            <div class="export-panel">
              <div class="panel-kicker">{{ t('settingsPage.exportData') }}</div>
              <h2>{{ t('settingsPage.exportCompleteBackup') }}</h2>
              <p>{{ t('settingsPage.exportCompleteBackupHelp') }}</p>
              <q-btn color="primary" unelevated no-caps icon="download"
                :loading="exporting" :label="t('settingsPage.exportCompleteBackup')"
                @click="exportCompleteBackupJson" />
            </div>

            <div class="import-panel">
              <div class="panel-kicker">{{ t('settingsPage.importData') }}</div>
              <h2>{{ t('settingsPage.importBackupTitle') }}</h2>
              <p>{{ t('settingsPage.importBackupHelp') }}</p>
              <div class="import-step">
                <span class="step-number">1</span>
                <div class="step-content">
                  <div class="step-title">{{ t('settingsPage.chooseBackupStep') }}</div>
                  <q-file v-model="backupFile" outlined dense accept=".json,application/json"
                    :label="t('settingsPage.chooseBackupFile')" />
                </div>
              </div>
              <q-banner v-if="backupPreview" rounded class="settings-note q-mt-md">
                {{ backupPreview }}
              </q-banner>
              <div class="import-step q-mt-md">
                <span class="step-number">2</span>
                <div class="step-content">
                  <div class="step-title">{{ t('settingsPage.chooseImportActionStep') }}</div>
                  <div class="import-actions">
                    <q-btn outline no-caps icon="visibility" :disable="!backupFile"
                      :label="t('settingsPage.previewImport')" @click="previewBackupImport" />
                    <q-btn outline no-caps icon="merge_type" :disable="!backupFile" :loading="importing"
                      :label="t('settingsPage.mergeImport')" @click="importBackup('merge')" />
                    <q-btn outline no-caps color="warning" icon="file_download"
                      :disable="!backupFile" :loading="importing"
                      :label="t('settingsPage.overwriteImport')" @click="confirmOverwriteSettings" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="management-section">
          <SettingsSectionHeading icon="o_cleaning_services" :title="t('settingsPage.maintenanceTitle')"
            :caption="t('settingsPage.maintenanceCaption')" />
          <div class="maintenance-list q-mt-md">
            <div v-for="action in maintenanceActions" :key="action.key" class="maintenance-item">
              <div class="action-icon"><q-icon :name="action.icon" size="22px" /></div>
              <div class="action-copy">
                <div class="action-title">{{ t(`settingsPage.${action.key}`) }}</div>
                <div class="action-description">{{ t(`settingsPage.${action.impactKey}`) }}</div>
              </div>
              <q-btn flat no-caps :icon="action.icon" :label="t(`settingsPage.${action.key}`)"
                @click="action.action" />
            </div>
          </div>
        </section>

        <section class="management-section danger-zone">
          <div class="danger-heading">
            <div class="danger-heading-icon"><q-icon name="o_warning" size="24px" /></div>
            <SettingsSectionHeading icon="o_delete_forever" :title="t('settingsPage.dangerZoneTitle')"
              :caption="t('settingsPage.dangerZoneCaption')" />
          </div>
          <div class="danger-list q-mt-md">
            <div class="danger-item">
              <div class="action-copy">
                <div class="action-title">{{ t('settingsPage.deleteAllSongs') }}</div>
                <div class="action-description">{{ t('settingsPage.deleteAllSongsImpact') }}</div>
              </div>
              <q-btn outline no-caps color="negative" icon="delete_sweep"
                :label="t('settingsPage.deleteAllSongs')"
                @click="confirmDanger('deleteAllSongsImpact', deleteAllSongs)" />
            </div>
            <div class="danger-item">
              <div class="action-copy">
                <div class="action-title">{{ t('settingsPage.deleteAccountData') }}</div>
                <div class="action-description">{{ t('settingsPage.deleteAccountDataImpact') }}</div>
              </div>
              <q-btn outline no-caps color="negative" icon="delete_forever"
                :label="t('settingsPage.deleteAccountData')"
                @click="confirmDanger('deleteAccountDataImpact', deleteAccountAndAllData)" />
            </div>
          </div>
        </section>
      </div>

      <div class="data-management-footer">
        <span>{{ t('settingsPage.dataManagementFooter') }}</span>
        <div class="footer-links">
          <router-link to="/my-vocabulary">{{ t('settingsPage.goToMyVocabulary') }}</router-link>
          <router-link to="/songs">{{ t('settingsPage.goToSongsManager') }}</router-link>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useQuasar } from 'quasar';
import SettingsSectionHeading from 'components/SettingsSectionHeading.vue';
import {
  ImportTaskResult,
  SongsService,
  UserVocabularyService,
  type SongImportRequest,
  type UserVocabulary,
  type UserVocabularyStats,
  type VocabularyStatus,
} from 'src/services/api';
import {
  APP_SETTINGS_STORAGE_KEY,
  loadAppSettings,
  normalizeAppSettings,
  saveAppSettings,
} from 'src/utils/appSettings';
import {
  applyMotionPreference,
  getStoredMotionPreference,
  MOTION_STORAGE_KEY,
  setMotionPreference,
  type MotionPreference,
} from 'src/utils/motionPreference';
import {
  backupSongs,
  backupVocabulary,
  downloadTextFile,
  timestampForFilename,
  type BackupPayload,
  type BackupVocabularyItem,
} from 'src/utils/settingsDataTransfer';

const { t } = useI18n();
const $q = useQuasar();

const settings = ref(loadAppSettings());
const motionPreference = ref<MotionPreference>(getStoredMotionPreference() ?? applyMotionPreference());
const songCount = ref<number | null>(null);
const vocabularyStats = ref<UserVocabularyStats | null>(null);
const backupFile = ref<File | null>(null);
const backupPreview = ref('');
const exporting = ref(false);
const importing = ref(false);

const maintenanceActions = [
  { key: 'clearSearchHistory', impactKey: 'clearSearchHistoryImpact', icon: 'history', action: clearSearchHistory },
  { key: 'clearLocalCache', impactKey: 'clearLocalCacheImpact', icon: 'cached', action: clearLocalCache },
];

const dataStats = computed(() => [
  { label: t('settingsPage.importedSongsCount'), value: songCount.value ?? '--' },
  { label: t('settingsPage.savedWordsCount'), value: vocabularyStats.value?.totalCount ?? '--' },
  { label: t('settingsPage.learningWordsStat'), value: vocabularyStats.value?.learningCount ?? '--' },
  { label: t('settingsPage.masteredWordsStat'), value: vocabularyStats.value?.masteredCount ?? '--' },
  { label: t('settingsPage.localDataSize'), value: estimateLocalStorageSize() },
]);

function buildBackupPayload(
  songs: Awaited<ReturnType<typeof SongsService.getAllSongs>>,
  vocabulary: Awaited<ReturnType<typeof UserVocabularyService.listUserVocabularyWords>>,
  stats: UserVocabularyStats | null,
) {
  return {
    schemaVersion: 1,
    exportedAt: new Date().toISOString(),
    app: { name: 'Lyric Vocabulary Builder', version: '0.0.1' },
    settings: settings.value,
    motionPreference: motionPreference.value,
    stats,
    songs,
    vocabulary,
  };
}

async function exportCompleteBackupJson() {
  exporting.value = true;
  try {
    const [songs, vocabulary, stats] = await Promise.all([
      SongsService.getAllSongs(),
      UserVocabularyService.listUserVocabularyWords(),
      UserVocabularyService.getUserVocabularyStats().catch(() => null),
    ]);
    downloadTextFile(
      `lyric-vocabulary-backup-${timestampForFilename()}.json`,
      JSON.stringify(buildBackupPayload(songs, vocabulary, stats), null, 2),
      'application/json;charset=utf-8',
    );
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.exportSuccess') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.exportFailed') });
  } finally {
    exporting.value = false;
  }
}

async function readBackupFile() {
  if (!backupFile.value) return null;
  try {
    const parsed: unknown = JSON.parse(await backupFile.value.text());
    if (typeof parsed !== 'object' || parsed === null) throw new Error('Invalid backup');
    return parsed as BackupPayload;
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.importPreviewFailed') });
    return null;
  }
}

async function previewBackupImport() {
  const backup = await readBackupFile();
  if (!backup) return;
  backupPreview.value = t('settingsPage.importPreviewSummary', {
    exportedAt: backup.exportedAt ?? t('unknown'),
    songs: backup.songs?.length ?? 0,
    vocabulary: backup.vocabulary?.length ?? 0,
    hasSettings: backup.settings ? t('yes') : t('no'),
  });
}

function applyImportedPreferences(backup: BackupPayload, replace = false) {
  const importedSettings = normalizeAppSettings(backup.settings);
  if (!importedSettings && !backup.motionPreference) return false;
  if (importedSettings) {
    settings.value = replace ? importedSettings : { ...settings.value, ...importedSettings };
    saveAppSettings(settings.value);
  }
  if (backup.motionPreference === 'on' || backup.motionPreference === 'off') {
    motionPreference.value = setMotionPreference(backup.motionPreference);
  }
  return true;
}

async function importBackup(mode: 'merge' | 'overwrite') {
  const backup = await readBackupFile();
  if (!backup) return;
  importing.value = true;
  try {
    if (mode === 'overwrite') {
      await deleteAllSongsData();
      await UserVocabularyService.clearUserVocabularyWords();
    }
    const preferencesApplied = applyImportedPreferences(backup, mode === 'overwrite');
    const songResult = await importSongsFromBackup(backupSongs(backup));
    const vocabularyCount = await importVocabularyFromBackup(backupVocabulary(backup));
    resetStatsAfterDataChange();
    $q.notify({
      type: songResult.failedCount > 0 ? 'warning' : 'positive',
      position: 'top-right',
      message: t('settingsPage.importBackupSuccess', {
        songs: songResult.successCount,
        failedSongs: songResult.failedCount,
        vocabulary: vocabularyCount,
        settings: preferencesApplied ? t('yes') : t('no'),
      }),
    });
  } catch (error) {
    console.error('Failed to import backup:', error);
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.importBackupFailed') });
  } finally {
    importing.value = false;
  }
}

async function confirmOverwriteSettings() {
  const backup = await readBackupFile();
  if (!backup) return;
  $q.dialog({
    title: t('settingsPage.highRiskConfirmTitle'),
    message: t('settingsPage.overwriteSettingsImpact'),
    cancel: true,
    persistent: true,
  }).onOk(() => void importBackup('overwrite'));
}

async function importSongsFromBackup(songs: SongImportRequest[]) {
  if (songs.length === 0) return { successCount: 0, failedCount: 0 };
  const task = await SongsService.importSongsAsync(songs);
  for (let index = 0; index < 30; index += 1) {
    const result = await SongsService.getImportTaskResult(task.taskId);
    if (result.status === ImportTaskResult.status.COMPLETED || result.status === ImportTaskResult.status.FAILED) {
      return { successCount: result.successCount, failedCount: result.failedCount };
    }
    await delay(500);
  }
  return { successCount: 0, failedCount: songs.length };
}

async function importVocabularyFromBackup(vocabulary: BackupVocabularyItem[]) {
  let imported = 0;
  for (const word of vocabulary) {
    const request: { lemma: string; note?: string | null } = { lemma: word.lemma };
    if (word.note !== undefined) request.note = word.note;
    const saved = await UserVocabularyService.addUserVocabularyWord(request);
    await restoreVocabularyState(saved, word);
    imported += 1;
  }
  return imported;
}

async function restoreVocabularyState(saved: UserVocabulary, item: BackupVocabularyItem) {
  if (!item.status && item.masteryScore === undefined && item.note === undefined) return;
  const request: { status?: VocabularyStatus; masteryScore?: number; note?: string | null } = {};
  if (item.status) request.status = item.status;
  if (item.masteryScore !== undefined) request.masteryScore = item.masteryScore;
  if (item.note !== undefined) request.note = item.note;
  await UserVocabularyService.updateUserVocabularyWord(saved.id, request);
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function resetStatsAfterDataChange() {
  void loadDataStats();
}

function clearSearchHistory() {
  window.localStorage.removeItem('lv-search-history');
  window.sessionStorage.removeItem('lv-search-history');
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.clearSuccess') });
}

function clearLocalCache() {
  window.sessionStorage.clear();
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.clearSuccess') });
}

async function deleteAllSongs() {
  try {
    await deleteAllSongsData();
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.clearSuccess') });
    resetStatsAfterDataChange();
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.clearFailed') });
  }
}

async function deleteAllSongsData() {
  const songs = await SongsService.getAllSongs();
  if (songs.length > 0) await SongsService.deleteSongsBatch(songs.map((song) => song.id));
}

async function deleteAccountAndAllData() {
  try {
    await deleteAllSongsData();
    await UserVocabularyService.clearUserVocabularyWords();
    window.localStorage.removeItem(APP_SETTINGS_STORAGE_KEY);
    window.localStorage.removeItem(MOTION_STORAGE_KEY);
    window.localStorage.removeItem('app-locale');
    window.sessionStorage.clear();
    settings.value = loadAppSettings();
    motionPreference.value = applyMotionPreference();
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.clearSuccess') });
    resetStatsAfterDataChange();
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.clearFailed') });
  }
}

function confirmDanger(messageKey: string, action: () => void | Promise<void>) {
  $q.dialog({
    title: t('settingsPage.highRiskConfirmTitle'),
    message: `${t(`settingsPage.${messageKey}`)}\n\n${t('settingsPage.highRiskConfirmFootnote')}`,
    cancel: true,
    persistent: true,
  }).onOk(() => void action());
}

function estimateLocalStorageSize() {
  const bytes = Object.entries(window.localStorage).reduce((total, [key, value]) => total + key.length + value.length, 0) * 2;
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function loadDataStats() {
  const [songs, stats] = await Promise.all([
    SongsService.getAllSongs().catch(() => null),
    UserVocabularyService.getUserVocabularyStats().catch(() => null),
  ]);
  songCount.value = songs?.length ?? null;
  vocabularyStats.value = stats;
}

onMounted(() => {
  void loadDataStats();
});
</script>

<style scoped lang="scss">
.data-management-content {
  max-width: 1120px;
}

.page-intro {
  margin-bottom: 28px;
}

.page-kicker,
.panel-kicker {
  color: var(--lv-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.page-title {
  margin: 6px 0 0;
  color: var(--lv-ink);
  font-size: clamp(34px, 6vw, 58px);
  line-height: 1;
}

.page-caption {
  max-width: 680px;
  margin: 12px 0 0;
  color: var(--lv-ink-soft);
  line-height: 1.6;
}

.management-sections {
  display: grid;
  gap: 18px;
}

.management-section {
  padding: 22px;
  background: var(--lv-surface-solid);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
  box-shadow: var(--lv-shadow-soft);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  min-width: 0;
  padding: 14px 16px;
  background: var(--lv-paper);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
}

.stat-value {
  color: var(--lv-ink);
  font-family: var(--lv-font-serif);
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  margin-top: 8px;
  color: var(--lv-ink-soft);
  font-size: 12px;
  line-height: 1.35;
}

.backup-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.75fr) minmax(0, 1.25fr);
  gap: 14px;
}

.export-panel,
.import-panel {
  padding: 18px;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
}

.export-panel {
  color: var(--lv-paper);
  background: var(--lv-brand-bg);
  border-color: transparent;
}

.export-panel .panel-kicker,
.export-panel p {
  color: rgba(255, 255, 255, 0.72);
}

.export-panel h2,
.import-panel h2 {
  margin: 8px 0 6px;
  color: inherit;
  font-size: 19px;
}

.export-panel p,
.import-panel p {
  margin: 0 0 18px;
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.55;
}

.export-panel .q-btn {
  color: var(--lv-brand-bg) !important;
  background: var(--lv-paper) !important;
}

.import-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-number {
  display: inline-grid;
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  place-items: center;
  color: var(--lv-paper);
  font-size: 12px;
  font-weight: 700;
  background: var(--lv-blue);
  border-radius: 50%;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-title {
  margin-bottom: 8px;
  color: var(--lv-ink);
  font-size: 13px;
  font-weight: 700;
}

.import-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.settings-note {
  color: var(--lv-ink-soft);
  background: var(--lv-accent-soft);
  border: 1px solid var(--lv-line);
}

.maintenance-list,
.danger-list {
  display: grid;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
  overflow: hidden;
}

.maintenance-item,
.danger-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--lv-paper);
}

.maintenance-item + .maintenance-item,
.danger-item + .danger-item {
  border-top: 1px solid var(--lv-line);
}

.action-icon,
.danger-heading-icon {
  display: inline-grid;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--lv-blue);
  background: var(--lv-accent-soft);
  border-radius: 50%;
}

.action-copy {
  flex: 1;
  min-width: 0;
}

.action-title {
  color: var(--lv-ink);
  font-weight: 700;
}

.action-description {
  margin-top: 4px;
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.45;
}

.danger-zone {
  background: color-mix(in srgb, var(--lv-warning-bg) 45%, var(--lv-surface-solid));
  border-color: color-mix(in srgb, var(--lv-danger) 32%, var(--lv-line));
}

.danger-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.danger-heading-icon {
  color: var(--lv-danger);
  background: var(--lv-warning-bg);
}

.danger-heading :deep(.section-heading) {
  flex: 1;
}

.danger-list {
  border-color: color-mix(in srgb, var(--lv-danger) 22%, var(--lv-line));
}

.danger-item {
  background: color-mix(in srgb, var(--lv-warning-bg) 32%, var(--lv-paper));
}

.data-management-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin: 16px 2px 4px;
  color: var(--lv-muted);
  font-size: 13px;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.footer-links a {
  color: var(--lv-blue);
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .backup-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .management-section {
    padding: 16px;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .maintenance-item,
  .danger-item,
  .data-management-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .maintenance-item .q-btn,
  .danger-item .q-btn {
    width: 100%;
  }
}
</style>
