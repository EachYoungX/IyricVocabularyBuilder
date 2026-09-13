<template>
  <q-page padding class="data-management-page">
    <div class="q-mx-auto data-management-content">
      <section class="page-masthead page-intro">
        <div class="lv-kicker">{{ t('settingsPage.dataKicker') }}</div>
        <h1 class="serif-display page-title">{{ t('settingsPage.dataTitle') }}</h1>
        <p class="page-caption lv-helper-text">{{ t('settingsPage.dataCaption') }}</p>
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
              <div class="lv-kicker">{{ t('settingsPage.exportData') }}</div>
              <h2>{{ t('settingsPage.exportCompleteBackup') }}</h2>
              <p>{{ t('settingsPage.exportCompleteBackupHelp') }}</p>
              <q-btn color="primary" unelevated no-caps icon="download"
                :loading="exporting" :label="t('settingsPage.exportCompleteBackup')"
                @click="exportCompleteBackupJson" />
            </div>

            <div class="import-panel">
              <div class="lv-kicker">{{ t('settingsPage.importData') }}</div>
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
                <div class="action-title">{{ t('settingsPage.clearAllLocalData') }}</div>
                <div class="action-description">{{ t('settingsPage.clearAllLocalDataImpact') }}</div>
              </div>
              <q-btn outline no-caps color="negative" icon="delete_forever"
                :label="t('settingsPage.clearAllLocalData')"
                @click="confirmDanger('clearAllLocalDataImpact', clearAllLocalData)" />
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
  DataService,
  SongsService,
  UserVocabularyService,
  type UserVocabularyStats,
} from 'src/services/api';
import { BackupApiService, type BackupPayload } from 'src/services/backupService';
import {
  applyAppSettings,
  loadAppSettings,
  saveAppSettings,
} from 'src/utils/appSettings';
import {
  applyMotionPreference,
  getStoredMotionPreference,
  setMotionPreference,
  type MotionPreference,
} from 'src/utils/motionPreference';
import {
  APP_LOCALE_STORAGE_KEY,
  clearClientPreferences,
  createClientPreferences,
  extractClientPreferences,
  loadKeptCleanupWords,
  saveKeptCleanupWords,
  SEARCH_HISTORY_STORAGE_KEY,
} from 'src/utils/clientPreferences';
import {
  downloadTextFile,
  timestampForFilename,
} from 'src/utils/settingsDataTransfer';

const { t, locale } = useI18n();
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

async function exportCompleteBackupJson() {
  exporting.value = true;
  try {
    const backendBackup = await BackupApiService.exportBackup();
    const backup: BackupPayload = {
      ...backendBackup,
      preferences: createClientPreferences(
        settings.value,
        motionPreference.value,
        locale.value,
        loadKeptCleanupWords(),
      ),
    };
    downloadTextFile(
      `lyric-vocabulary-backup-${timestampForFilename()}.json`,
      JSON.stringify(backup, null, 2),
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
  try {
    const validation = await BackupApiService.validateBackup(backup);
    backupPreview.value = t('settingsPage.importPreviewSummary', {
      exportedAt: backup.exportedAt,
      songs: validation.songCount,
      vocabulary: validation.userVocabularyCount,
      hasSettings: extractClientPreferences(backup)?.settings ? t('yes') : t('no'),
    });
  } catch {
    backupPreview.value = '';
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.importPreviewFailed') });
  }
}

function applyImportedPreferences(backup: BackupPayload, replace = false) {
  const imported = extractClientPreferences(backup);
  if (!imported) return false;
  if (imported.settings) {
    settings.value = replace ? imported.settings : { ...settings.value, ...imported.settings };
    saveAppSettings(settings.value);
  }
  if (imported.motionPreference) {
    motionPreference.value = setMotionPreference(imported.motionPreference);
  }
  if (imported.locale) {
    locale.value = imported.locale;
    window.localStorage.setItem(APP_LOCALE_STORAGE_KEY, imported.locale);
  }
  if (imported.keptCleanupWords) {
    saveKeptCleanupWords(imported.keptCleanupWords);
  }
  return true;
}

async function importBackup() {
  const backup = await readBackupFile();
  if (!backup) return;
  importing.value = true;
  try {
    const result = await BackupApiService.restoreBackup(backup);
    const preferencesApplied = applyImportedPreferences(backup, true);
    resetStatsAfterDataChange();
    $q.notify({
      type: 'positive',
      position: 'top-right',
      message: t('settingsPage.importBackupSuccess', {
        songs: result.restoredSongs,
        failedSongs: 0,
        vocabulary: result.restoredUserVocabulary,
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
  }).onOk(() => void importBackup());
}

function resetStatsAfterDataChange() {
  void loadDataStats();
}

function clearSearchHistory() {
  window.localStorage.removeItem(SEARCH_HISTORY_STORAGE_KEY);
  window.sessionStorage.removeItem(SEARCH_HISTORY_STORAGE_KEY);
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

async function clearAllLocalData() {
  try {
    if (window.desktopBridge) {
      await window.desktopBridge.clearAllLocalData();
      return;
    }
    await DataService.clearAllLocalData();
    clearClientPreferences();
    window.sessionStorage.clear();
    locale.value = 'en-US';
    settings.value = loadAppSettings();
    applyAppSettings(settings.value);
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
  const [count, stats] = await Promise.all([
    SongsService.countSongs().catch(() => null),
    UserVocabularyService.getUserVocabularyStats().catch(() => null),
  ]);
  songCount.value = count;
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

.page-title {
  margin: 6px 0 0;
  color: var(--lv-ink);
  font-size: clamp(34px, 6vw, 58px);
  line-height: 1;
}

.page-caption {
  max-width: 680px;
  margin: 12px 0 0;
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
  color: var(--lv-on-brand);
  background: var(--lv-brand-bg);
  border-color: transparent;
}

.export-panel .lv-kicker,
.export-panel p {
  color: color-mix(in srgb, var(--lv-on-brand) 72%, transparent);
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
