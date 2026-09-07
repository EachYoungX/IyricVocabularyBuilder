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
                <SettingsSelect v-model="settings.themeMode" outlined emit-value map-options :options="themeOptions"
                  :label="t('settingsPage.themeMode')" @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="motionPreference" outlined emit-value map-options :options="motionOptions"
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
                <SettingsSelect v-model="settings.defaultNewWordStatus" outlined emit-value map-options
                  :options="newWordStatusOptions" :label="t('settingsPage.defaultNewWordStatus')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="settings.lowValueWordHandling" outlined emit-value map-options
                  :options="lowValueOptions" :label="t('settingsPage.lowValueWordHandling')"
                  @update:model-value="persistSettings" />
              </div>
            </div>
            <q-expansion-item dense expand-separator class="settings-info q-mt-md" icon="o_help_outline"
              :label="t('settingsPage.lowValueExplanationTitle')">
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
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_library_music" :title="t('settingsPage.lyricsTitle')"
              :caption="t('settingsPage.lyricsCaption')" />
            <div class="row q-col-gutter-md q-mt-sm">
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="settings.postImportBehavior" outlined emit-value map-options
                  :options="postImportOptions" :label="t('settingsPage.postImportBehavior')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="settings.roleLabelHandling" outlined emit-value map-options
                  :options="roleLabelOptions" :label="t('settingsPage.roleLabelHandling')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="settings.repeatedChorusHandling" outlined emit-value map-options
                  :options="repeatedChorusOptions" :label="t('settingsPage.repeatedChorusHandling')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12 col-md-6">
                <SettingsSelect v-model="settings.fillerWordHandling" outlined emit-value map-options
                  :options="fillerWordOptions" :label="t('settingsPage.fillerWordHandling')"
                  @update:model-value="persistSettings" />
              </div>
              <div class="col-12">
                <q-toggle v-model="settings.autoAddImportedWords" color="primary"
                  :label="t('settingsPage.autoAddImportedWords')" @update:model-value="persistSettings" />
                <div class="settings-help">{{ t('settingsPage.autoAddImportedWordsHelp') }}</div>
              </div>
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_storage" :title="t('settingsPage.datasetTitle')"
              :caption="t('settingsPage.datasetCaption')" />
            <q-linear-progress v-if="datasetBusy" indeterminate rounded color="primary" class="q-mt-md" />
            <template v-if="isDesktop">
              <q-banner rounded class="settings-info q-mt-md">
                {{ t('settingsPage.externalDatasetWarning') }}
              </q-banner>
              <div class="dataset-status q-mt-md">
                <div>
                  <div class="about-label">{{ t('settingsPage.activeDataset') }}</div>
                  <div class="text-subtitle1 text-weight-medium">
                    {{ datasetState?.active?.fileName || t('settingsPage.noActiveDataset') }}
                  </div>
                  <div v-if="datasetState?.active" class="settings-help">
                    {{ datasetSourceLabel(datasetState.active.source) }} ·
                    {{ datasetStatusLabel(datasetState.active.status) }}
                    <template v-if="datasetState.active.datasetVersion">
                      · {{ t('settingsPage.datasetVersion', { version: datasetState.active.datasetVersion }) }}
                    </template>
                  </div>
                </div>
                <q-chip :color="datasetState?.dictionaryEnabled ? 'positive' : 'warning'" text-color="white">
                  {{ datasetState?.dictionaryEnabled
                    ? t('settingsPage.dictionaryEnabled')
                    : t('settingsPage.dictionaryDisabled') }}
                </q-chip>
              </div>
              <div class="row q-gutter-sm q-mt-md">
                <q-btn outline color="primary" icon="o_folder_open" :label="t('settingsPage.openDatasetDirectory')"
                  :disable="datasetBusy" @click="openDatasetDirectory" />
                <q-btn outline color="primary" icon="o_refresh" :label="t('settingsPage.rescanDatasets')"
                  :disable="datasetBusy" @click="rescanDatasets" />
                <q-btn color="primary" icon="o_drive_folder_upload" :label="t('settingsPage.importManagedDataset')"
                  :disable="datasetBusy" @click="importManagedDataset" />
                <q-btn outline color="primary" icon="o_insert_drive_file"
                  :label="t('settingsPage.selectExternalDataset')" :disable="datasetBusy"
                  @click="selectExternalDataset" />
              </div>
              <q-list v-if="datasetState?.managed.length" bordered separator class="settings-list q-mt-md">
                <q-item v-for="dataset in datasetState.managed" :key="dataset.path">
                  <q-item-section>
                    <q-item-label>{{ dataset.fileName }}</q-item-label>
                    <q-item-label caption>
                      {{ datasetStatusLabel(dataset.status) }} · {{ formatDatasetSize(dataset.size) }}
                      <template v-if="dataset.datasetVersion">
                        · {{ t('settingsPage.datasetVersion', { version: dataset.datasetVersion }) }}
                      </template>
                    </q-item-label>
                    <q-item-label v-if="dataset.message" caption class="text-negative">
                      {{ dataset.message }}
                    </q-item-label>
                  </q-item-section>
                  <q-item-section side class="row items-center no-wrap q-gutter-xs">
                    <q-btn v-if="dataset.status === 'valid' && !isActiveManagedDataset(dataset)" flat dense
                      color="primary" icon="o_check_circle" :label="t('settingsPage.useDataset')"
                      :disable="datasetBusy" @click="activateManagedDataset(dataset.fileName)" />
                    <q-chip v-else-if="isActiveManagedDataset(dataset)" dense color="positive" text-color="white">
                      {{ t('settingsPage.inUse') }}
                    </q-chip>
                    <q-btn flat round dense color="negative" icon="o_delete" :aria-label="t('settingsPage.removeDataset')"
                      :disable="datasetBusy" @click="removeManagedDataset(dataset.fileName)" />
                  </q-item-section>
                </q-item>
              </q-list>
              <div v-else class="settings-help q-mt-md">{{ t('settingsPage.noManagedDatasets') }}</div>
              <q-btn v-if="datasetState?.active?.source === 'external'" flat color="negative" class="q-mt-sm"
                icon="o_link_off" :label="t('settingsPage.clearExternalDataset')" :disable="datasetBusy"
                @click="clearExternalDataset" />
            </template>
            <q-banner v-else rounded class="settings-info q-mt-md">
              {{ t('settingsPage.datasetHostOnly') }}
            </q-banner>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="settings-card">
          <q-card-section>
            <SettingsSectionHeading icon="o_wifi" :title="t('settingsPage.lanTitle')"
              :caption="t('settingsPage.lanCaption')" />
            <q-linear-progress v-if="lanBusy" indeterminate rounded color="primary" class="q-mt-md" />
            <template v-if="isDesktop">
              <q-toggle :model-value="runtimeInfo?.lanEnabled ?? false" color="primary" class="q-mt-md"
                :label="t('settingsPage.lanEnabled')" :disable="lanBusy || !runtimeInfo"
                @update:model-value="setLanEnabled" />
              <q-banner rounded class="settings-info q-mt-md">
                <template #avatar><q-icon name="o_warning_amber" color="warning" /></template>
                {{ t('settingsPage.lanSecurityWarning') }}
              </q-banner>
              <template v-if="runtimeInfo?.lanEnabled">
                <div class="about-label q-mt-md">{{ t('settingsPage.lanAddresses') }}</div>
                <q-list v-if="runtimeInfo.lanUrls.length" bordered separator class="settings-list q-mt-sm">
                  <q-item v-for="url in runtimeInfo.lanUrls" :key="url">
                    <q-item-section>
                      <q-item-label class="text-weight-medium">{{ url }}</q-item-label>
                    </q-item-section>
                  </q-item>
                </q-list>
                <div v-else class="settings-help q-mt-sm">{{ t('settingsPage.lanNoAddress') }}</div>
              </template>
            </template>
            <q-banner v-else rounded class="settings-info q-mt-md">
              {{ t('settingsPage.lanHostOnly') }}
            </q-banner>
          </q-card-section>
        </q-card>

        <SettingsSection icon="o_menu_book" :title="t('settingsPage.dictionaryDisplaySectionTitle')"
          :caption="t('settingsPage.dictionaryDisplaySectionCaption')">
          <SettingRow :title="t('settingsPage.definitionLanguage')">
            <SettingsSelect v-model="settings.definitionLanguage" class="setting-select" outlined dense emit-value
              map-options :options="definitionLanguageOptions" :aria-label="t('settingsPage.definitionLanguage')"
              @update:model-value="persistSettings" />
          </SettingRow>
          <SettingRow :title="t('settingsPage.definitionMode')" :description="t('settingsPage.definitionModeHelp')">
            <q-option-group :model-value="definitionMode" :options="definitionModeOptions" type="radio" inline
              color="primary" class="definition-mode-options" @update:model-value="setDefinitionMode" />
          </SettingRow>
          <SettingRow :title="t('settingsPage.displayContent')" :description="t('settingsPage.displayContentHelp')">
            <q-option-group :model-value="displayContent" :options="displayContentOptions" type="checkbox" inline
              color="primary" class="display-content-options" @update:model-value="setDisplayContent" />
          </SettingRow>
        </SettingsSection>

        <SettingsSection icon="o_manage_search" :title="t('settingsPage.searchRecognitionSectionTitle')"
          :caption="t('settingsPage.searchRecognitionSectionCaption')">
          <SettingRow :title="t('settingsPage.lemmaSearch')" :description="t('settingsPage.lemmaSearchHelp')">
            <q-toggle v-model="settings.lemmaSearch" color="primary" :aria-label="t('settingsPage.lemmaSearch')"
              @update:model-value="persistSettings" />
          </SettingRow>
          <SettingRow :title="t('settingsPage.phraseDetection')"
            :description="t('settingsPage.phraseDetectionHelp')">
            <q-toggle v-model="settings.phraseDetection" color="primary"
              :aria-label="t('settingsPage.phraseDetection')" @update:model-value="persistSettings" />
          </SettingRow>
        </SettingsSection>

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
                <div class="about-value">{{ appVersion }}</div>
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
import SettingRow from 'components/SettingRow.vue';
import SettingsSection from 'components/SettingsSection.vue';
import SettingsSectionHeading from 'components/SettingsSectionHeading.vue';
import SettingsSelect from 'components/SettingsSelect.vue';
import {
  loadAppSettings,
  saveAppSettings,
  type AppSettings,
  type DictionaryDisplayItem,
} from 'src/utils/appSettings';
import {
  applyMotionPreference,
  getStoredMotionPreference,
  setMotionPreference,
  type MotionPreference,
} from 'src/utils/motionPreference';

type Option<T extends string | boolean = string> = { label: string; value: T };

const { t } = useI18n();
const $q = useQuasar();
const settings = ref<AppSettings>(loadAppSettings());
const motionPreference = ref<MotionPreference>(getStoredMotionPreference() ?? applyMotionPreference());
const appVersion = process.env.APP_VERSION;
const desktopBridge = window.desktopBridge;
const isDesktop = Boolean(desktopBridge);
const datasetState = ref<DesktopDatasetState | null>(null);
const datasetBusy = ref(false);
const runtimeInfo = ref<DesktopRuntimeInfo | null>(null);
const lanBusy = ref(false);

const option = <T extends string | boolean>(key: string, value: T): Option<T> => ({
  label: t(`settingsPage.${key}`),
  value,
});

const themeOptions = computed(() => [
  option('themeMidnightSail', 'midnight-sail'),
  option('themeSageLibrary', 'sage-library'),
  option('themeCoralStudy', 'coral-study'),
  option('themeDuskMinimal', 'dusk-minimal'),
  option('themeMidnightDark', 'midnight-sail-dark'),
]);
const motionOptions = computed(() => [option('motionOn', 'on'), option('motionOff', 'off')]);
const newWordStatusOptions = computed(() => [option('newWordNew', 'NEW'), option('newWordLearning', 'LEARNING')]);
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
const repeatedChorusOptions = computed(() => [option('chorusKeep', 'KEEP_ALL'), option('chorusDedupe', 'DEDUP_LEARNING_STATS')]);
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
const definitionModeOptions = computed(() => [
  option('dictBrief', 'BRIEF'),
  option('dictFull', 'FULL'),
]);
const displayContentOptions = computed(() => [
  option('dictPhoneticPos', 'PHONETIC_POS'),
  option('dictInflections', 'INFLECTIONS'),
  option('dictLyricContext', 'LYRIC_CONTEXT'),
]);
const definitionMode = computed<'BRIEF' | 'FULL'>(() =>
  settings.value.dictionaryDisplay.includes('FULL')
    && !settings.value.dictionaryDisplay.includes('BRIEF') ? 'FULL' : 'BRIEF',
);
const displayContent = computed<DictionaryDisplayItem[]>(() => settings.value.dictionaryDisplay.filter((item) =>
  item === 'PHONETIC_POS' || item === 'INFLECTIONS' || item === 'LYRIC_CONTEXT',
));

const lowValueExplanationItems = [
  'lowValueFactorFillers',
  'lowValueFactorShort',
  'lowValueFactorRepeated',
  'lowValueStillSearchable',
];
const privacyItems = [
  'lyricsUserImported',
  'noLyricCrawler',
  'legalUseNotice',
  'dictionaryDisclaimer',
  'localStorageNotice',
  'exportDeleteNotice',
];

function persistSettings() {
  saveAppSettings(settings.value);
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.settingsSaved') });
}

function persistMotion(value: MotionPreference) {
  motionPreference.value = setMotionPreference(value);
  $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.settingsSaved') });
}

function setDefinitionMode(value: 'BRIEF' | 'FULL') {
  const content = settings.value.dictionaryDisplay.filter(
    (item) => item !== 'BRIEF' && item !== 'FULL',
  );
  settings.value.dictionaryDisplay = [value, ...content];
  persistSettings();
}

function setDisplayContent(value: DictionaryDisplayItem[]) {
  settings.value.dictionaryDisplay = [definitionMode.value, ...value];
  persistSettings();
}

onMounted(() => {
  void loadDesktopDatasetState();
  void loadDesktopRuntimeInfo();
});

async function loadDesktopRuntimeInfo() {
  if (!desktopBridge) return;
  try {
    runtimeInfo.value = await desktopBridge.getRuntimeInfo();
  } catch (error) {
    notifyDatasetError(error);
  }
}

async function setLanEnabled(enabled: boolean) {
  if (!desktopBridge) return;
  lanBusy.value = true;
  try {
    runtimeInfo.value = await desktopBridge.setLanEnabled(enabled);
    $q.notify({
      type: 'positive',
      position: 'top-right',
      message: t(enabled ? 'settingsPage.lanEnabledMessage' : 'settingsPage.lanDisabledMessage'),
    });
  } catch (error) {
    notifyDatasetError(error);
  } finally {
    lanBusy.value = false;
  }
}

async function loadDesktopDatasetState() {
  if (!desktopBridge) return;
  try {
    datasetState.value = await desktopBridge.getDatasetState();
    if (datasetState.value.autoSelected && datasetState.value.active) {
      $q.notify({
        type: 'info',
        position: 'top-right',
        message: t('settingsPage.datasetAutoSelected', { fileName: datasetState.value.active.fileName }),
      });
    }
  } catch (error) {
    notifyDatasetError(error);
  }
}

async function openDatasetDirectory() {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    await desktopBridge.openDatasetDirectory();
  }, false);
}

async function rescanDatasets() {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    datasetState.value = await desktopBridge.rescanDatasets();
  });
}

async function importManagedDataset() {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    const result = await desktopBridge.importDatasetToManagedDirectory();
    if (result) datasetState.value = result;
  });
}

async function selectExternalDataset() {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    const result = await desktopBridge.selectExternalDataset();
    if (result) datasetState.value = result;
  });
}

async function activateManagedDataset(fileName: string) {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    datasetState.value = await desktopBridge.activateManagedDataset(fileName);
  });
}

async function clearExternalDataset() {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    datasetState.value = await desktopBridge.clearExternalDataset();
  });
}

async function removeManagedDataset(fileName: string) {
  if (!desktopBridge) return;
  await runDatasetAction(async () => {
    datasetState.value = await desktopBridge.removeManagedDataset(fileName);
  });
}

async function runDatasetAction(action: () => Promise<void>, notifySuccess = true) {
  datasetBusy.value = true;
  try {
    await action();
    if (notifySuccess) {
      $q.notify({ type: 'positive', position: 'top-right', message: t('settingsPage.datasetActionComplete') });
    }
  } catch (error) {
    notifyDatasetError(error);
  } finally {
    datasetBusy.value = false;
  }
}

function notifyDatasetError(error: unknown) {
  $q.notify({
    type: 'negative',
    position: 'top-right',
    message: error instanceof Error ? error.message : t('settingsPage.datasetActionFailed'),
  });
}

function datasetSourceLabel(source: DesktopDatasetDescriptor['source']) {
  return t(source === 'managed' ? 'settingsPage.datasetManaged' : 'settingsPage.datasetExternal');
}

function datasetStatusLabel(status: DesktopDatasetDescriptor['status']) {
  return t(`settingsPage.datasetStatus${status[0]!.toUpperCase()}${status.slice(1)}`);
}

function isActiveManagedDataset(dataset: DesktopDatasetDescriptor) {
  return datasetState.value?.active?.source === 'managed'
    && datasetState.value.active.path === dataset.path
    && datasetState.value.active.status === 'valid';
}

function formatDatasetSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
</script>

<style scoped lang="scss">
.settings-content {
  max-width: 1120px;
}

.settings-title {
  margin: 0;
  color: var(--lv-ink);
  font-size: clamp(34px, 6vw, 58px);
  line-height: 1;
}

.settings-grid {
  display: grid;
  gap: 18px;
}

.settings-card,
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

.settings-page :deep(.setting-select) {
  width: min(300px, 100%);
}

.settings-page :deep(.definition-mode-options),
.settings-page :deep(.display-content-options) {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px 18px;
}

.settings-page :deep(.definition-mode-options .q-radio),
.settings-page :deep(.display-content-options .q-checkbox) {
  margin: 0;
}

.settings-info {
  background: var(--lv-surface);
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-sm);
}

.settings-info p {
  margin: 0 0 8px;
}

.dataset-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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
  margin-top: 10px;
  color: var(--lv-ink);
  font-size: clamp(18px, 2.4vw, 23px);
  font-weight: 600;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

@media (max-width: 700px) {
  .settings-page :deep(.definition-mode-options),
  .settings-page :deep(.display-content-options) {
    justify-content: flex-start;
  }

  .settings-page :deep(.setting-select) {
    width: 100%;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
