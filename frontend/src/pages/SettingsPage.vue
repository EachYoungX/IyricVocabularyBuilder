<template>
  <q-page padding class="settings-page">
    <div class="q-mx-auto settings-content">
      <section class="page-masthead q-mb-lg">
        <h1 class="serif-display settings-title">{{ t('settingsPage.title') }}</h1>
      </section>

      <div class="settings-grid">
        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_palette" :title="t('settingsPage.appearanceTitle')"
              :caption="t('settingsPage.appearanceCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div class="col-12 col-md-6">
                <q-select v-model="settings.fontScale" outlined emit-value map-options :options="fontScaleOptions"
                  :label="t('settingsPage.fontScale')" @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.interfaceScale" outlined emit-value map-options :options="interfaceScaleOptions"
                  :label="t('settingsPage.interfaceScale')" @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.themeMode" outlined emit-value map-options :options="themeOptions"
                  :label="t('settingsPage.themeMode')" @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="motionPreference" outlined emit-value map-options :options="motionOptions"
                  :label="t('settingsPage.motionEffects')" @update:model-value="persistMotion" />
              </div>
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_school" :title="t('settingsPage.learningTitle')"
              :caption="t('settingsPage.learningCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div class="col-12 col-md-6">
                <q-select v-model="settings.defaultNewWordStatus" outlined emit-value map-options
                  :options="newWordStatusOptions" :label="t('settingsPage.defaultNewWordStatus')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.lowValueWordHandling" outlined emit-value map-options
                  :options="lowValueOptions" :label="t('settingsPage.lowValueWordHandling')"
                  @update:model-value="persistSettings" />
              </div>
            </div>
            <q-expansion-item
              dense
              expand-separator
              class="settings-info q-mt-md"
              icon="o_help_outline"
              :label="t('settingsPage.lowValueExplanationTitle')"
            >
              <div class="settings-help q-pa-md">
                <p>{{ t('settingsPage.lowValueExplanationBody') }}</p>
                <q-list dense>
                  <q-item v-for="item in lowValueExplanationItems" :key="item">
                    <q-item-section avatar>
                      <q-icon name="fiber_manual_record" size="8px" color="primary" />
                    </q-item-section>
                    <q-item-section>{{ t(`settingsPage.${item}`) }}</q-item-section>
                  </q-item>
                </q-list>
              </div>
            </q-expansion-item>
            <q-banner rounded class="settings-note q-mt-md">
              {{ t('settingsPage.reviewTimingDeferred') }}
            </q-banner>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_library_music" :title="t('settingsPage.lyricsTitle')"
              :caption="t('settingsPage.lyricsCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div class="col-12 col-md-6">
                <q-select v-model="settings.postImportBehavior" outlined emit-value map-options
                  :options="postImportOptions" :label="t('settingsPage.postImportBehavior')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.roleLabelHandling" outlined emit-value map-options
                  :options="roleLabelOptions" :label="t('settingsPage.roleLabelHandling')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.repeatedChorusHandling" outlined emit-value map-options
                  :options="repeatedChorusOptions" :label="t('settingsPage.repeatedChorusHandling')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-select v-model="settings.fillerWordHandling" outlined emit-value map-options
                  :options="fillerWordOptions" :label="t('settingsPage.fillerWordHandling')"
                  @update:model-value="persistSettings" />
              </div>
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_menu_book" :title="t('settingsPage.dictionaryTitle')"
              :caption="t('settingsPage.dictionaryCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div class="col-12 col-md-6">
                <q-select v-model="settings.definitionLanguage" outlined emit-value map-options
                  :options="definitionLanguageOptions" :label="t('settingsPage.definitionLanguage')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-option-group v-model="settings.dictionaryDisplay" :options="dictionaryDisplayOptions" type="checkbox"
                  color="primary" @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <q-toggle v-model="settings.lemmaSearch" color="primary" :label="t('settingsPage.lemmaSearch')"
                  @update:model-value="persistSettings" />
                <div class="settings-help">{{ t('settingsPage.lemmaSearchHelp') }}</div>
              </div>
              <div class="col-12 col-md-6">
                <q-toggle v-model="settings.phraseDetection" color="primary" :label="t('settingsPage.phraseDetection')"
                  @update:model-value="persistSettings" />
              </div>
            </div>

            <q-separator class="q-my-lg" />

            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <div class="settings-subhead">{{ t('settingsPage.vocabularyImportExport') }}</div>
                <q-chip v-for="format in importExportFormats" :key="format" outline color="primary">{{ format }}</q-chip>
              </div>
              <div class="col-12 col-md-6">
                <div class="settings-subhead">{{ t('settingsPage.exportScope') }}</div>
                <q-list dense bordered separator class="settings-list">
                  <q-item v-for="scope in exportScopes" :key="scope">
                    <q-item-section>{{ t(`settingsPage.${scope}`) }}</q-item-section>
                  </q-item>
                </q-list>
              </div>
            </div>

            <div class="dictionary-source-block">
              <div class="settings-subhead">{{ t('dictionarySourceTitle') }}</div>
              <div v-if="dictionaryLoading" class="text-center q-py-md">
                <q-spinner color="primary" size="28px" />
              </div>
              <div v-else-if="dictionarySource" class="q-gutter-sm">
                <div class="source-row">
                  <span>{{ t('dictionarySourceName') }}</span>
                  <strong>{{ dictionarySource.sourceName }}</strong>
                </div>
                <a v-if="dictionarySource.sourceUrl" class="source-link" :href="dictionarySource.sourceUrl" target="_blank">
                  {{ dictionarySource.sourceUrl }}
                </a>
                <div class="source-row">
                  <span>{{ t('dictionaryLicenseName') }}</span>
                  <strong>{{ dictionarySource.licenseName }}</strong>
                </div>
                <div class="source-row">
                  <span>{{ t('requiresAttribution') }}</span>
                  <strong>{{ yesNo(dictionarySource.requiresAttribution) }}</strong>
                </div>
                <div class="source-row">
                  <span>{{ t('commercialUseAllowed') }}</span>
                  <strong>{{ yesNo(dictionarySource.commercialUseAllowed) }}</strong>
                </div>
                <div class="source-row">
                  <span>{{ t('redistributionAllowed') }}</span>
                  <strong>{{ yesNo(dictionarySource.redistributionAllowed) }}</strong>
                </div>
                <p class="settings-help q-mt-sm">{{ dictionarySource.attributionText }}</p>
              </div>
              <div v-else class="text-negative">{{ t('dictionarySourceLoadFailed') }}</div>
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_storage" :title="t('settingsPage.dataTitle')"
              :caption="t('settingsPage.dataCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div v-for="item in dataStats" :key="item.label" class="col-6 col-md-3">
                <q-card flat bordered class="stat-card">
                  <div class="stat-value">{{ item.value }}</div>
                  <div class="stat-label">{{ item.label }}</div>
                </q-card>
              </div>
            </div>

            <div class="row q-col-gutter-md q-mt-md">
              <div class="col-12 col-md-4">
                <div class="settings-subhead">{{ t('settingsPage.exportData') }}</div>
                <q-btn outline no-caps class="settings-action" :loading="exporting"
                  :label="t('settingsPage.exportVocabulary')" @click="exportVocabularyCsv" />
                <q-btn outline no-caps class="settings-action" :loading="exporting"
                  :label="t('settingsPage.exportAnki')" @click="exportVocabularyAnkiTsv" />
                <q-btn outline no-caps class="settings-action" :loading="exporting"
                  :label="t('settingsPage.exportLearningRecords')" @click="exportLearningRecordsJson" />
                <q-btn outline no-caps class="settings-action" :loading="exporting"
                  :label="t('settingsPage.exportCompleteBackup')" @click="exportCompleteBackupJson" />
              </div>
              <div class="col-12 col-md-4">
                <div class="settings-subhead">{{ t('settingsPage.importData') }}</div>
                <q-file v-model="backupFile" outlined dense accept=".json,application/json"
                  :label="t('settingsPage.chooseBackupFile')" class="q-mb-sm" />
                <q-file v-model="vocabularyImportFile" outlined dense accept=".csv,.tsv,text/csv,text/tab-separated-values,text/plain"
                  :label="t('settingsPage.chooseVocabularyFile')" class="q-mb-sm" />
                <q-banner v-if="backupPreview" rounded class="settings-note q-mb-sm">
                  {{ backupPreview }}
                </q-banner>
                <q-btn outline no-caps class="settings-action" :disable="!backupFile || importing"
                  :label="t('settingsPage.previewImport')" @click="previewBackupImport" />
                <q-btn outline no-caps class="settings-action" :disable="!backupFile" :loading="importing"
                  :label="t('settingsPage.mergeImport')" @click="importBackup('merge')" />
                <q-btn outline no-caps class="settings-action" :disable="!backupFile" :loading="importing"
                  :label="t('settingsPage.overwriteImport')" @click="confirmOverwriteSettings" />
                <q-btn outline no-caps class="settings-action" :disable="!vocabularyImportFile" :loading="importing"
                  :label="t('settingsPage.importVocabularyFile')" @click="importVocabularyFile" />
              </div>
              <div class="col-12 col-md-4">
                <div class="settings-subhead">{{ t('settingsPage.clearData') }}</div>
                <q-btn v-for="action in clearActions" :key="action.key" outline no-caps color="negative"
                  class="settings-action" :label="t(`settingsPage.${action.key}`)"
                  @click="confirmDanger(action.messageKey, action.action)" />
              </div>
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_privacy_tip" :title="t('settingsPage.privacyTitle')"
              :caption="t('settingsPage.privacyCaption')" />
            <q-list bordered separator class="settings-list q-mt-md">
              <q-item v-for="item in privacyItems" :key="item">
                <q-item-section avatar>
                  <q-icon name="check_circle" color="primary" />
                </q-item-section>
                <q-item-section>{{ t(`settingsPage.${item}`) }}</q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_info" :title="t('settingsPage.aboutTitle')"
              :caption="t('settingsPage.aboutCaption')" />
            <div class="about-grid q-mt-md">
              <div class="about-tile">
                <div class="about-label">{{ t('settingsPage.version') }}</div>
                <div class="about-value">0.0.1</div>
              </div>
              <a class="about-tile about-link" href="https://github.com/EachYoungX/IyricVocabularyBuilder" target="_blank">
                <div class="about-label">{{ t('settingsPage.github') }}</div>
                <div class="about-value">EachYoungX/IyricVocabularyBuilder</div>
              </a>
              <a class="about-tile about-link" href="https://github.com/EachYoungX/IyricVocabularyBuilder/releases"
                target="_blank">
                <div class="about-label">{{ t('settingsPage.download') }}</div>
                <div class="about-value">{{ t('settingsPage.releasePage') }}</div>
              </a>
              <a class="about-tile about-link"
                href="https://github.com/EachYoungX/IyricVocabularyBuilder/blob/main/CHANGELOG.md" target="_blank">
                <div class="about-label">{{ t('settingsPage.changelog') }}</div>
                <div class="about-value">CHANGELOG.md</div>
              </a>
            </div>
          </q-card-section>
        </q-card>
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
  DictionaryService,
  ImportTaskResult,
  SongsService,
  UserVocabularyService,
  type DictionarySource,
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
  type AppSettings,
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
  parseVocabularyText,
  timestampForFilename,
  vocabularyToAnkiTsv,
  vocabularyToCsv,
  type BackupPayload,
  type BackupVocabularyItem,
} from 'src/utils/settingsDataTransfer';

type Option<T extends string | boolean = string> = {
  label: string;
  value: T;
};

const { t } = useI18n();
const $q = useQuasar();

const settings = ref<AppSettings>(loadAppSettings());
const motionPreference = ref<MotionPreference>(getStoredMotionPreference() ?? applyMotionPreference());
const dictionaryLoading = ref(false);
const dictionarySource = ref<DictionarySource | null>(null);
const songCount = ref<number | null>(null);
const vocabularyStats = ref<UserVocabularyStats | null>(null);
const backupFile = ref<File | null>(null);
const vocabularyImportFile = ref<File | null>(null);
const backupPreview = ref('');
const exporting = ref(false);
const importing = ref(false);

const option = <T extends string | boolean>(key: string, value: T): Option<T> => ({
  label: t(`settingsPage.${key}`),
  value,
});

const fontScaleOptions = computed(() => [
  option('scaleCompact', 'compact'),
  option('scaleStandard', 'standard'),
  option('scaleLarge', 'large'),
]);
const interfaceScaleOptions = fontScaleOptions;
const themeOptions = computed(() => [
  option('themeMidnightSail', 'midnight-sail'),
  option('themeSageLibrary', 'sage-library'),
  option('themeCoralStudy', 'coral-study'),
  option('themeDuskMinimal', 'dusk-minimal'),
  option('themeMidnightDark', 'midnight-sail-dark'),
]);
const motionOptions = computed(() => [option('motionOn', 'on'), option('motionOff', 'off')]);
const newWordStatusOptions = computed(() => [
  option('newWordNew', 'NEW'),
  option('newWordLearning', 'LEARNING'),
]);
const lowValueOptions = computed(() => [
  option('lowValueQueryOnly', 'QUERY_ONLY'),
  option('lowValueNormal', 'NORMAL'),
  option('lowValueHideMarker', 'HIDE_RECOMMENDATION_MARKER'),
]);
const postImportOptions = computed(() => [option('postImportSave', 'SAVE_DIRECTLY'), option('postImportConfirm', 'CONFIRM_CLEANING')]);
const roleLabelOptions = computed(() => [
  option('roleAutoHide', 'AUTO_HIDE'),
  option('roleAutoDelete', 'AUTO_DELETE'),
  option('roleKeep', 'KEEP_VISIBLE'),
  option('roleConfirm', 'CONFIRM_EACH_IMPORT'),
]);
const repeatedChorusOptions = computed(() => [
  option('chorusKeep', 'KEEP_ALL'),
  option('chorusDedupe', 'DEDUP_LEARNING_STATS'),
]);
const fillerWordOptions = computed(() => [
  option('fillerNotRecommended', 'NOT_RECOMMENDED'),
  option('fillerNormal', 'NORMAL'),
  option('fillerExclude', 'EXCLUDE_STATS'),
]);
const definitionLanguageOptions = computed(() => [
  option('definitionZh', 'ZH'),
  option('definitionEn', 'EN'),
  option('definitionBilingual', 'BILINGUAL'),
]);
const dictionaryDisplayOptions = computed(() => [
  option('dictBrief', 'BRIEF'),
  option('dictFull', 'FULL'),
  option('dictPhoneticPos', 'PHONETIC_POS'),
  option('dictInflections', 'INFLECTIONS'),
  option('dictLyricContext', 'LYRIC_CONTEXT'),
]);

const importExportFormats = ['CSV', 'JSON', 'Anki', t('settingsPage.backupPackage')];
const exportScopes = ['exportPersonalVocabulary', 'exportVocabularyWithStatus', 'exportFullBackup'];
const lowValueExplanationItems = [
  'lowValueFactorFillers',
  'lowValueFactorShort',
  'lowValueFactorRepeated',
  'lowValueStillSearchable',
];
const clearActions = [
  { key: 'clearSearchHistory', messageKey: 'clearSearchHistoryImpact', action: clearSearchHistory },
  { key: 'clearLocalCache', messageKey: 'clearLocalCacheImpact', action: clearLocalCache },
  { key: 'deleteAllSongs', messageKey: 'deleteAllSongsImpact', action: deleteAllSongs },
  { key: 'deleteLearningRecords', messageKey: 'deleteLearningRecordsImpact', action: deleteLearningRecords },
  { key: 'deleteAccountData', messageKey: 'deleteAccountDataImpact', action: deleteAccountAndAllData },
];
const privacyItems = [
  'lyricsUserImported',
  'noLyricCrawler',
  'legalUseNotice',
  'dictionaryLicenseNotice',
  'localStorageNotice',
  'exportDeleteNotice',
];

const dataStats = computed(() => [
  { label: t('settingsPage.importedSongsCount'), value: songCount.value ?? '--' },
  { label: t('settingsPage.savedWordsCount'), value: vocabularyStats.value?.totalCount ?? '--' },
  { label: t('settingsPage.learningWordsStat'), value: vocabularyStats.value?.learningCount ?? '--' },
  { label: t('settingsPage.masteredWordsStat'), value: vocabularyStats.value?.masteredCount ?? '--' },
  { label: t('settingsPage.localDataSize'), value: estimateLocalStorageSize() },
]);

function yesNo(value?: boolean) {
  if (value === undefined) return t('unknown');
  return value ? t('yes') : t('no');
}

function persistSettings() {
  saveAppSettings(settings.value);
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.settingsSaved') });
}

function persistMotion(value: MotionPreference) {
  motionPreference.value = setMotionPreference(value);
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.settingsSaved') });
}

function buildBackupPayload(
  songs: Awaited<ReturnType<typeof SongsService.getAllSongs>>,
  vocabulary: Awaited<ReturnType<typeof UserVocabularyService.listUserVocabularyWords>>,
  stats: UserVocabularyStats | null,
) {
  return {
    schemaVersion: 1,
    exportedAt: new Date().toISOString(),
    app: {
      name: 'Lyric Vocabulary Builder',
      version: '0.0.1',
    },
    settings: settings.value,
    motionPreference: motionPreference.value,
    dictionarySource: dictionarySource.value,
    stats,
    songs,
    vocabulary,
  };
}

async function exportVocabularyCsv() {
  exporting.value = true;
  try {
    const words = await UserVocabularyService.listUserVocabularyWords();
    downloadTextFile(
      `lyric-vocabulary-${timestampForFilename()}.csv`,
      vocabularyToCsv(words),
      'text/csv;charset=utf-8',
    );
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.exportSuccess') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.exportFailed') });
  } finally {
    exporting.value = false;
  }
}

async function exportVocabularyAnkiTsv() {
  exporting.value = true;
  try {
    const words = await UserVocabularyService.listUserVocabularyWords();
    downloadTextFile(
      `lyric-vocabulary-anki-${timestampForFilename()}.tsv`,
      await vocabularyToAnkiTsv(words),
      'text/tab-separated-values;charset=utf-8',
    );
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.exportSuccess') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.exportFailed') });
  } finally {
    exporting.value = false;
  }
}

async function exportLearningRecordsJson() {
  exporting.value = true;
  try {
    const [vocabulary, stats] = await Promise.all([
      UserVocabularyService.listUserVocabularyWords(),
      UserVocabularyService.getUserVocabularyStats().catch(() => null),
    ]);
    downloadTextFile(
      `lyric-learning-records-${timestampForFilename()}.json`,
      JSON.stringify({ schemaVersion: 1, exportedAt: new Date().toISOString(), stats, vocabulary }, null, 2),
      'application/json;charset=utf-8',
    );
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.exportSuccess') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.exportFailed') });
  } finally {
    exporting.value = false;
  }
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
  if (!importedSettings && !backup.motionPreference) {
    return false;
  }

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
  }).onOk(() => {
    void importBackup('overwrite');
  });
}

async function importSongsFromBackup(songs: SongImportRequest[]) {
  if (songs.length === 0) return { successCount: 0, failedCount: 0 };
  const task = await SongsService.importSongsAsync(songs);
  for (let index = 0; index < 30; index += 1) {
    const result = await SongsService.getImportTaskResult(task.taskId);
    if (result.status === ImportTaskResult.status.COMPLETED || result.status === ImportTaskResult.status.FAILED) {
      return {
        successCount: result.successCount,
        failedCount: result.failedCount,
      };
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

async function importVocabularyFile() {
  if (!vocabularyImportFile.value) return;
  importing.value = true;
  try {
    const vocabulary = parseVocabularyText(
      await vocabularyImportFile.value.text(),
      vocabularyImportFile.value.name,
    );
    const imported = await importVocabularyFromBackup(vocabulary);
    resetStatsAfterDataChange();
    $q.notify({
      type: 'positive',
      position: 'top-right',
      message: t('settingsPage.importVocabularySuccess', { count: imported }),
    });
    vocabularyImportFile.value = null;
  } catch (error) {
    console.error('Failed to import vocabulary file:', error);
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.importVocabularyFailed') });
  } finally {
    importing.value = false;
  }
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
  void loadSettingsData();
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
  if (songs.length > 0) {
    await SongsService.deleteSongsBatch(songs.map((song) => song.id));
  }
}

async function deleteLearningRecords() {
  try {
    await deleteLearningRecordsData();
    $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.clearSuccess') });
    resetStatsAfterDataChange();
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('settingsPage.clearFailed') });
  }
}

async function deleteLearningRecordsData() {
  await UserVocabularyService.clearUserVocabularyWords();
}

async function deleteAccountAndAllData() {
  try {
    await deleteAllSongsData();
    await deleteLearningRecordsData();
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
  }).onOk(() => {
    void action();
  });
}

function estimateLocalStorageSize() {
  const bytes = Object.entries(window.localStorage).reduce((total, [key, value]) => total + key.length + value.length, 0) * 2;
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

async function loadSettingsData() {
  dictionaryLoading.value = true;
  try {
    const [source, songs, stats] = await Promise.all([
      DictionaryService.getDictionarySource().catch(() => null),
      SongsService.getAllSongs().catch(() => null),
      UserVocabularyService.getUserVocabularyStats().catch(() => null),
    ]);
    dictionarySource.value = source;
    songCount.value = songs?.length ?? null;
    vocabularyStats.value = stats;
  } finally {
    dictionaryLoading.value = false;
  }
}

onMounted(() => {
  void loadSettingsData();
});
</script>

<style scoped lang="scss">
.settings-content {
  max-width: 1120px;
}

.settings-title {
  color: var(--lv-ink);
  margin: 0;
  font-size: clamp(34px, 6vw, 58px);
  line-height: 1;
}

.settings-grid {
  display: grid;
  gap: 18px;
}

.settings-card,
.stat-card,
.settings-list {
  background: var(--lv-surface-solid);
  border-color: var(--lv-line);
  box-shadow: var(--lv-shadow-soft);
}

.settings-help {
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.6;
}

.settings-note {
  color: var(--lv-ink-soft);
  background: var(--lv-accent-soft);
  border: 1px solid var(--lv-line);
}

.settings-info {
  background: rgba(255, 255, 255, 0.48);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
}

.settings-info p {
  margin: 0 0 8px;
}

.settings-subhead {
  color: var(--lv-ink);
  font-weight: 700;
  margin-bottom: 8px;
}

.dictionary-source-block {
  margin-top: 22px;
  padding: 16px;
  background: var(--lv-accent-soft);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
}

.source-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: var(--lv-ink-soft);
}

.source-row strong {
  color: var(--lv-ink);
  text-align: right;
}

.source-link {
  display: inline-block;
  color: var(--lv-blue);
  overflow-wrap: anywhere;
}

.stat-card {
  padding: 16px;
}

.stat-value {
  color: var(--lv-ink);
  font-family: var(--lv-font-serif);
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  color: var(--lv-ink-soft);
  font-size: 12px;
}

.settings-action {
  width: 100%;
  justify-content: flex-start;
  margin-bottom: 8px;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.about-tile {
  display: block;
  min-height: 104px;
  padding: 18px 20px;
  color: var(--lv-ink);
  text-decoration: none;
  background: var(--lv-surface-solid);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
}

.about-link {
  transition:
    border-color var(--lv-motion-fast) var(--lv-motion-ease),
    transform var(--lv-motion-fast) var(--lv-motion-ease);
}

.about-link:hover {
  border-color: var(--lv-line-strong);
  transform: translateY(-1px);
}

.about-label {
  color: var(--lv-ink-soft);
  font-size: 13px;
  line-height: 1.4;
}

.about-value {
  color: var(--lv-ink);
  margin-top: 10px;
  font-size: clamp(18px, 2.4vw, 23px);
  font-weight: 600;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

@media (max-width: 700px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
