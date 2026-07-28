<template>
  <q-page class="my-vocabulary-page q-pa-md">
    <div class="page-head row items-start justify-between q-gutter-md">
      <div>
        <div class="text-h5 text-weight-bold">{{ t('myVocabularyTitle') }}</div>
        <div class="text-body2 text-grey-7">{{ t('myVocabularySubtitle') }}</div>
      </div>
      <q-btn flat round icon="refresh" :loading="store.isLoading" @click="loadData">
        <q-tooltip>{{ t('refresh') }}</q-tooltip>
      </q-btn>
    </div>

    <div class="stats-row q-mt-md">
      <div v-for="stat in statsCards" :key="stat.key" class="stat-tile">
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
      </div>
    </div>

    <q-expansion-item
      class="quality-panel q-mt-md"
      icon="import_export"
      :label="t('vocabularyDataManagement')"
      :caption="t('vocabularyDataManagementHint')"
    >
      <div class="q-pa-md data-actions">
        <div class="row q-col-gutter-md">
          <div class="col-12 col-md-6">
            <div class="section-label">{{ t('exportData') }}</div>
            <div class="row q-gutter-sm">
              <q-btn outline no-caps icon="download" :loading="isMutating" :label="t('exportVocabularyCsv')" @click="exportVocabularyCsv" />
              <q-btn outline no-caps icon="download" :loading="isMutating" :label="t('exportAnki')" @click="exportVocabularyAnkiTsv" />
              <q-btn outline no-caps icon="download" :loading="isMutating" :label="t('exportLearningRecords')" @click="exportLearningRecordsJson" />
            </div>
          </div>
          <div class="col-12 col-md-6">
            <div class="section-label">{{ t('importData') }}</div>
            <div class="row q-col-gutter-sm items-start">
              <div class="col-12 col-sm">
                <q-file
                  v-model="vocabularyImportFile"
                  outlined
                  dense
                  clearable
                  accept=".csv,.tsv,text/csv,text/tab-separated-values,text/plain"
                  :label="t('chooseVocabularyFile')"
                />
              </div>
              <div class="col-12 col-sm-auto">
                <q-btn outline no-caps icon="upload" :disable="!vocabularyImportFile" :loading="isMutating" :label="t('importVocabularyFile')" @click="importVocabularyFile" />
              </div>
            </div>
          </div>
        </div>
        <q-separator class="q-my-md" />
        <q-btn flat color="negative" icon="delete_forever" :loading="isMutating" :label="t('deleteAllLearningRecords')" @click="confirmClearVocabulary" />
      </div>
    </q-expansion-item>

    <q-expansion-item
      class="quality-panel q-mt-md"
      icon="rule"
      :label="t('vocabularyCleanupCandidates')"
      :caption="t('vocabularyCleanupCandidatesHint')"
      default-opened
    >
      <div class="q-pa-md">
        <div class="row items-center justify-between q-gutter-sm q-mb-sm">
          <div class="text-body2 text-grey-7">
            {{ t('qualityCandidateCount', { count: qualityCandidates.length }) }}
            <span v-if="keptCleanupWords.size > 0" class="q-ml-sm">
              {{ t('keptCleanupCandidateCount', { count: keptCleanupWords.size }) }}
            </span>
          </div>
          <div class="row q-gutter-sm">
            <q-btn
              v-if="keptCleanupWords.size > 0"
              flat
              dense
              icon="undo"
              :label="t('clearKeptCleanupCandidates')"
              @click="clearKeptCandidates"
            />
            <q-btn
              flat
              dense
              icon="visibility_off"
              :disable="selectedQualityCandidates.length === 0"
              :loading="isMutating"
              :label="t('ignoreSelectedCandidates')"
              @click="confirmIgnoreCandidates(selectedQualityCandidates)"
            />
            <q-btn
              flat
              dense
              color="negative"
              icon="delete"
              :disable="selectedQualityCandidates.length === 0"
              :loading="isMutating"
              :label="t('deleteSelectedCandidates')"
              @click="confirmDeleteCandidates(selectedQualityCandidates)"
            />
            <q-btn flat dense icon="refresh" :loading="qualityLoading" :label="t('refresh')" @click="loadQualityCandidates" />
          </div>
        </div>
        <div class="cleanup-summary q-mb-sm">
          <div v-for="item in cleanupSummaryCards" :key="item.key" class="cleanup-summary-item">
            <div class="cleanup-summary-value">{{ item.value }}</div>
            <div class="cleanup-summary-label">{{ item.label }}</div>
          </div>
        </div>
        <q-table
          v-model:selected="selectedQualityCandidates"
          flat
          bordered
          dense
          row-key="word"
          selection="multiple"
          :rows="qualityCandidates"
          :columns="qualityColumns"
          :loading="qualityLoading"
          :rows-per-page-options="[8, 15, 30]"
          :pagination="{ rowsPerPage: 8 }"
        >
          <template #body-cell-word="props">
            <q-td :props="props">
              <button type="button" class="word-button" @click="lookupCandidate(props.row.word)">
                {{ props.row.word }}
              </button>
            </q-td>
          </template>
          <template #body-cell-reasons="props">
            <q-td :props="props">
              <q-chip
                v-for="reason in props.row.reasons"
                :key="reason"
                dense
                size="sm"
                color="amber-2"
                text-color="brown-8"
              >
                {{ qualityReasonLabel(reason) }}
              </q-chip>
            </q-td>
          </template>
          <template #body-cell-example="props">
            <q-td :props="props">
              <span class="candidate-example">{{ props.row.examples?.[0]?.lyricLine ?? '-' }}</span>
            </q-td>
          </template>
          <template #body-cell-actions="props">
            <q-td :props="props">
              <q-btn flat round dense icon="check" @click.stop="keepCandidate(props.row.word)">
                <q-tooltip>{{ t('keepCandidate') }}</q-tooltip>
              </q-btn>
              <q-btn flat round dense icon="visibility_off" @click.stop="confirmIgnoreCandidates([props.row])">
                <q-tooltip>{{ t('ignoreCandidate') }}</q-tooltip>
              </q-btn>
              <q-btn flat round dense color="negative" icon="delete" @click.stop="confirmDeleteCandidates([props.row])">
                <q-tooltip>{{ t('deleteCandidate') }}</q-tooltip>
              </q-btn>
            </q-td>
          </template>
        </q-table>
      </div>
    </q-expansion-item>

    <div class="toolbar row items-center q-col-gutter-sm q-mt-md">
      <div class="col-12 col-md-4">
        <q-input v-model="searchText" dense outlined clearable debounce="150" :placeholder="t('searchPersonalVocabulary')">
          <template #prepend>
            <q-icon name="search" />
          </template>
        </q-input>
      </div>
      <div class="col-12 col-md-3">
        <q-select
          v-model="statusFilter"
          dense
          outlined
          emit-value
          map-options
          :options="filterOptions"
          :label="t('statusFilter')"
        />
      </div>
      <div class="col-12 col-md">
        <div class="row justify-end q-gutter-sm">
          <q-select
            v-model="batchStatus"
            dense
            outlined
            emit-value
            map-options
            :disable="selected.length === 0"
            :options="statusOptions"
            :label="t('batchStatus')"
            class="batch-select"
          />
          <q-btn
            color="primary"
            unelevated
            icon="done_all"
            :disable="selected.length === 0 || !batchStatus"
            :loading="isMutating"
            :label="t('apply')"
            @click="applyBatchStatus"
          />
          <q-btn
            color="negative"
            flat
            icon="delete"
            :disable="selected.length === 0"
            :loading="isMutating"
            :label="t('removeSelected')"
            @click="confirmRemoveSelected"
          />
        </div>
      </div>
    </div>

    <q-splitter v-model="splitter" class="content-splitter q-mt-md" :limits="[45, 72]">
      <template #before>
        <q-table
          v-model:selected="selected"
          flat
          bordered
          row-key="id"
          selection="multiple"
          :rows="filteredRows"
          :columns="columns"
          :loading="store.isLoading"
          :rows-per-page-options="[15, 30, 50, 0]"
          :pagination="{ rowsPerPage: 15 }"
          @row-click="handleRowClick"
        >
          <template #body-cell-lemma="props">
            <q-td :props="props">
              <button type="button" class="word-button" @click.stop="selectWord(props.row)">
                {{ props.row.lemma }}
              </button>
            </q-td>
          </template>

          <template #body-cell-status="props">
            <q-td :props="props">
              <q-select
                dense
                borderless
                emit-value
                map-options
                :model-value="props.row.status"
                :options="statusOptions"
                @update:model-value="(value: VocabularyStatus) => updateStatus(props.row.id, value)"
              />
            </q-td>
          </template>

          <template #body-cell-actions="props">
            <q-td :props="props">
              <q-btn flat round dense icon="visibility" @click.stop="selectWord(props.row)">
                <q-tooltip>{{ t('viewOccurrences') }}</q-tooltip>
              </q-btn>
              <q-btn flat round dense color="negative" icon="delete" @click.stop="confirmRemoveOne(props.row)">
                <q-tooltip>{{ t('removeFromVocabulary') }}</q-tooltip>
              </q-btn>
            </q-td>
          </template>
        </q-table>
      </template>

      <template #after>
        <div class="detail-pane q-pa-md">
          <div v-if="!activeWord" class="empty-state">
            <q-icon name="o_menu_book" size="44px" />
            <div>{{ t('selectWordToViewOccurrences') }}</div>
          </div>
          <template v-else>
            <div class="row items-start justify-between q-gutter-sm">
              <div>
                <div class="text-h6">{{ activeWord.lemma }}</div>
                <q-badge outline color="primary">{{ statusLabel(activeWord.status) }}</q-badge>
              </div>
              <q-btn flat round dense icon="refresh" :loading="occurrencesLoading" @click="loadOccurrences(activeWord.lemma)" />
            </div>

            <q-separator class="q-my-md" />

            <div v-if="occurrencesLoading" class="text-grey-7">{{ t('loadingOccurrences') }}</div>
            <div v-else-if="occurrenceError" class="text-negative">{{ occurrenceError }}</div>
            <div v-else-if="occurrences.length === 0" class="text-grey-7">{{ t('noOccurrencesFound') }}</div>
            <q-list v-else separator>
              <q-item v-for="(occurrence, index) in occurrences" :key="`${occurrence.songId}-${occurrence.lineIndex}-${index}`" class="occurrence-item">
                <q-item-section>
                  <q-item-label class="text-weight-medium">{{ occurrence.songTitle }}</q-item-label>
                  <q-item-label caption>{{ t('lineNumber', { number: displayLineNumber(occurrence.lineIndex) }) }}</q-item-label>
                  <q-item-label class="lyric-line q-mt-xs">{{ occurrence.lyricLine }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </template>
        </div>
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Dialog, Notify, type QTableColumn } from 'quasar';
import { useUserVocabularyStore } from 'src/stores/userVocabularyStore';
import {
  VocabularyService,
  VocabularyStatus,
  UserVocabularyService,
  type UserVocabulary,
  type UserVocabularyRequest,
  type UserVocabularyUpdateRequest,
  type VocabularyQualityCandidate,
  type WordOccurrence,
} from 'src/services/api';

const { t } = useI18n();
const store = useUserVocabularyStore();
const KEPT_CLEANUP_WORDS_STORAGE_KEY = 'lv-kept-cleanup-candidate-words';

const splitter = ref(58);
const searchText = ref('');
const statusFilter = ref<VocabularyStatus | 'ALL'>('ALL');
const batchStatus = ref<VocabularyStatus | null>(null);
const selected = ref<UserVocabulary[]>([]);
const selectedQualityCandidates = ref<VocabularyQualityCandidate[]>([]);
const keptCleanupWords = ref(loadKeptCleanupWords());
const activeWord = ref<UserVocabulary | null>(null);
const occurrences = ref<WordOccurrence[]>([]);
const qualityCandidates = ref<VocabularyQualityCandidate[]>([]);
const vocabularyImportFile = ref<File | null>(null);
const occurrencesLoading = ref(false);
const qualityLoading = ref(false);
const occurrenceError = ref('');
const isMutating = ref(false);

const activeStatuses = [
  VocabularyStatus.NEW,
  VocabularyStatus.LEARNING,
  VocabularyStatus.MASTERED,
  VocabularyStatus.IGNORED,
];

const statusOptions = computed(() =>
  activeStatuses.map((status) => ({
    label: statusLabel(status),
    value: status,
  })),
);

const filterOptions = computed(() => [
  { label: t('allStatuses'), value: 'ALL' },
  ...statusOptions.value,
]);

const columns = computed<QTableColumn<UserVocabulary>[]>(() => [
  { name: 'lemma', label: t('word'), field: 'lemma', align: 'left', sortable: true },
  { name: 'status', label: t('status'), field: 'status', align: 'left', sortable: true },
  {
    name: 'masteryScore',
    label: t('masteryScore'),
    field: (row) => Math.round(row.masteryScore * 100),
    align: 'right',
    sortable: true,
    format: (value) => `${value}%`,
  },
  { name: 'lastSeenAt', label: t('lastSeen'), field: 'lastSeenAt', align: 'left', sortable: true, format: formatDate },
  { name: 'actions', label: t('actions'), field: 'id', align: 'right' },
]);

const qualityColumns = computed<QTableColumn<VocabularyQualityCandidate>[]>(() => [
  { name: 'word', label: t('word'), field: 'word', align: 'left', sortable: true },
  {
    name: 'learningScore',
    label: t('learningScore'),
    field: (row) => Math.round(row.learningScore * 100),
    align: 'right',
    sortable: true,
    format: (value) => `${value}%`,
  },
  { name: 'occurrenceCount', label: t('frequency'), field: 'occurrenceCount', align: 'right', sortable: true },
  { name: 'reasons', label: t('candidateReasons'), field: 'reasons', align: 'left' },
  { name: 'example', label: t('lyricsPreview'), field: 'examples', align: 'left' },
  { name: 'actions', label: t('actions'), field: 'word', align: 'right' },
]);

const filteredRows = computed<UserVocabulary[]>(() => {
  const query = searchText.value.trim().toLowerCase();
  return store.words.filter((word: UserVocabulary) => {
    const matchesStatus = statusFilter.value === 'ALL' || word.status === statusFilter.value;
    const matchesQuery = query === '' || word.lemma.toLowerCase().includes(query);
    return matchesStatus && matchesQuery;
  });
});

const statsCards = computed(() => [
  { key: 'total', label: t('totalWordsLabel'), value: store.stats?.totalCount ?? 0 },
  { key: 'new', label: statusLabel(VocabularyStatus.NEW), value: store.stats?.newCount ?? 0 },
  { key: 'learning', label: statusLabel(VocabularyStatus.LEARNING), value: store.stats?.learningCount ?? 0 },
  { key: 'mastered', label: statusLabel(VocabularyStatus.MASTERED), value: store.stats?.masteredCount ?? 0 },
  { key: 'ignored', label: statusLabel(VocabularyStatus.IGNORED), value: store.stats?.ignoredCount ?? 0 },
]);

const cleanupImpact = computed(() => summarizeCandidates(qualityCandidates.value));
const selectedCleanupImpact = computed(() => summarizeCandidates(selectedQualityCandidates.value));
const cleanupSummaryCards = computed(() => [
  { key: 'totalCandidates', label: t('cleanupTotalCandidates'), value: cleanupImpact.value.count },
  { key: 'totalOccurrences', label: t('cleanupTotalOccurrences'), value: cleanupImpact.value.occurrences },
  { key: 'totalSongs', label: t('cleanupTotalSongs'), value: cleanupImpact.value.songs },
  { key: 'selectedCandidates', label: t('cleanupSelectedCandidates'), value: selectedCleanupImpact.value.count },
  { key: 'selectedOccurrences', label: t('cleanupSelectedOccurrences'), value: selectedCleanupImpact.value.occurrences },
  { key: 'selectedSongs', label: t('cleanupSelectedSongs'), value: selectedCleanupImpact.value.songs },
]);

onMounted(() => {
  void loadData();
});

async function loadData() {
  await Promise.all([store.fetchDashboard(), loadQualityCandidates()]);
}

function statusLabel(status: VocabularyStatus) {
  return t(`vocabularyStatuses.${status}`);
}

function formatDate(value: string) {
  if (!value) {
    return '';
  }
  return value.slice(0, 16).replace('T', ' ');
}

function displayLineNumber(lineIndex?: number) {
  return lineIndex === undefined ? '-' : lineIndex + 1;
}

async function selectWord(word: UserVocabulary) {
  activeWord.value = word;
  await loadOccurrences(word.lemma);
}

function handleRowClick(_: Event, row: UserVocabulary) {
  void selectWord(row);
}

async function loadOccurrences(lemma: string) {
  occurrencesLoading.value = true;
  occurrenceError.value = '';
  try {
    occurrences.value = await VocabularyService.getWordOccurrences(lemma);
  } catch {
    occurrences.value = [];
    occurrenceError.value = t('loadOccurrencesFailed');
  } finally {
    occurrencesLoading.value = false;
  }
}

async function loadQualityCandidates() {
  qualityLoading.value = true;
  try {
    const candidates = await VocabularyService.getVocabularyQualityCandidates(80);
    qualityCandidates.value = candidates.filter((candidate) => !keptCleanupWords.value.has(candidate.word));
    selectedQualityCandidates.value = [];
  } catch {
    Notify.create({ type: 'negative', message: t('loadQualityCandidatesFailed') });
  } finally {
    qualityLoading.value = false;
  }
}

async function exportVocabularyCsv() {
  isMutating.value = true;
  try {
    const rows = await UserVocabularyService.listUserVocabularyWords();
    downloadTextFile(`lyric-vocabulary-${timestampForFilename()}.csv`, buildVocabularyCsv(rows), 'text/csv;charset=utf-8');
    Notify.create({ type: 'positive', message: t('exportSuccess') });
  } catch {
    Notify.create({ type: 'negative', message: t('exportFailed') });
  } finally {
    isMutating.value = false;
  }
}

async function exportVocabularyAnkiTsv() {
  isMutating.value = true;
  try {
    const rows = await UserVocabularyService.listUserVocabularyWords();
    const body = rows.map((word) => [word.lemma, statusLabel(word.status), word.note ?? ''].map(tsvCell).join('\t')).join('\n');
    downloadTextFile(`lyric-vocabulary-anki-${timestampForFilename()}.tsv`, body, 'text/tab-separated-values;charset=utf-8');
    Notify.create({ type: 'positive', message: t('exportSuccess') });
  } catch {
    Notify.create({ type: 'negative', message: t('exportFailed') });
  } finally {
    isMutating.value = false;
  }
}

async function exportLearningRecordsJson() {
  isMutating.value = true;
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
    Notify.create({ type: 'positive', message: t('exportSuccess') });
  } catch {
    Notify.create({ type: 'negative', message: t('exportFailed') });
  } finally {
    isMutating.value = false;
  }
}

async function importVocabularyFile() {
  if (!vocabularyImportFile.value) {
    return;
  }
  isMutating.value = true;
  try {
    const parsed = parseVocabularyText(await vocabularyImportFile.value.text(), vocabularyImportFile.value.name);
    for (const item of parsed) {
      const addRequest: UserVocabularyRequest = { lemma: item.lemma };
      if (item.note !== undefined) addRequest.note = item.note;
      const saved = await UserVocabularyService.addUserVocabularyWord(addRequest);
      if (item.status || item.masteryScore !== undefined || item.note !== undefined) {
        const updateRequest: UserVocabularyUpdateRequest = {};
        if (item.status) updateRequest.status = item.status;
        if (item.masteryScore !== undefined) updateRequest.masteryScore = item.masteryScore;
        if (item.note !== undefined) updateRequest.note = item.note;
        await UserVocabularyService.updateUserVocabularyWord(saved.id, updateRequest);
      }
    }
    vocabularyImportFile.value = null;
    await store.fetchDashboard();
    Notify.create({ type: 'positive', message: t('importVocabularySuccess', { count: parsed.length }) });
  } catch {
    Notify.create({ type: 'negative', message: t('importVocabularyFailed') });
  } finally {
    isMutating.value = false;
  }
}

function confirmClearVocabulary() {
  Dialog.create({
    title: t('deleteAllLearningRecords'),
    message: t('deleteAllLearningRecordsImpact'),
    cancel: true,
    persistent: true,
    ok: { color: 'negative', label: t('deleteForever') },
  }).onOk(() => void clearVocabulary());
}

async function clearVocabulary() {
  isMutating.value = true;
  try {
    await UserVocabularyService.clearUserVocabularyWords();
    selected.value = [];
    activeWord.value = null;
    occurrences.value = [];
    await store.fetchDashboard();
    Notify.create({ type: 'positive', message: t('clearSuccess') });
  } catch {
    Notify.create({ type: 'negative', message: t('clearFailed') });
  } finally {
    isMutating.value = false;
  }
}

function lookupCandidate(word: string) {
  const existing = store.words.find((item: UserVocabulary) => item.lemma === word);
  if (existing) {
    void selectWord(existing);
    return;
  }
  activeWord.value = {
    id: -1,
    userId: 'local',
    lemma: word,
    status: VocabularyStatus.IGNORED,
    masteryScore: 0,
    firstSeenAt: '',
    lastSeenAt: '',
  };
  void loadOccurrences(word);
}

function qualityReasonLabel(reason: string) {
  return t(`qualityReasons.${reason}`);
}

function keepCandidate(word: string) {
  keptCleanupWords.value = new Set([...keptCleanupWords.value, word]);
  saveKeptCleanupWords(keptCleanupWords.value);
  qualityCandidates.value = qualityCandidates.value.filter((candidate) => candidate.word !== word);
  selectedQualityCandidates.value = selectedQualityCandidates.value.filter((candidate) => candidate.word !== word);
  Notify.create({ type: 'positive', message: t('candidateKept') });
}

function clearKeptCandidates() {
  keptCleanupWords.value = new Set();
  saveKeptCleanupWords(keptCleanupWords.value);
  void loadQualityCandidates();
}

function confirmDeleteCandidates(candidates: VocabularyQualityCandidate[]) {
  const words = uniqueCandidateWords(candidates);
  if (words.length === 0) {
    return;
  }
  Dialog.create({
    title: t('deleteCleanupCandidates'),
    message: cleanupImpactMessage('deleteCleanupCandidatesImpact', candidates),
    cancel: true,
    persistent: true,
    ok: { color: 'negative', label: t('deleteForever') },
  }).onOk(() => void deleteCandidates(words));
}

async function deleteCandidates(words: string[]) {
  isMutating.value = true;
  try {
    const deletedCount = await VocabularyService.deleteVocabularyWords({ words });
    removeQualityCandidates(words);
    Notify.create({
      type: 'positive',
      message: t('cleanupCandidatesDeleted', { count: deletedCount, remaining: qualityCandidates.value.length }),
    });
  } catch {
    Notify.create({ type: 'negative', message: t('cleanupCandidatesDeleteFailed') });
  } finally {
    isMutating.value = false;
  }
}

function confirmIgnoreCandidates(candidates: VocabularyQualityCandidate[]) {
  const words = uniqueCandidateWords(candidates);
  if (words.length === 0) {
    return;
  }
  Dialog.create({
    title: t('ignoreCleanupCandidates'),
    message: cleanupImpactMessage('ignoreCleanupCandidatesImpact', candidates),
    cancel: true,
    persistent: true,
    ok: { color: 'primary', label: t('ignore') },
  }).onOk(() => void ignoreCandidates(words));
}

async function ignoreCandidates(words: string[]) {
  isMutating.value = true;
  try {
    for (const word of words) {
      const existing = store.words.find((item: UserVocabulary) => item.lemma === word);
      const saved = existing ?? await UserVocabularyService.addUserVocabularyWord({ lemma: word });
      await UserVocabularyService.updateUserVocabularyWord(saved.id, {
        status: VocabularyStatus.IGNORED,
        masteryScore: 0,
      });
    }
    removeQualityCandidates(words);
    await store.fetchDashboard();
    Notify.create({
      type: 'positive',
      message: t('cleanupCandidatesIgnored', { count: words.length, remaining: qualityCandidates.value.length }),
    });
  } catch {
    Notify.create({ type: 'negative', message: t('cleanupCandidatesIgnoreFailed') });
  } finally {
    isMutating.value = false;
  }
}

function uniqueCandidateWords(candidates: VocabularyQualityCandidate[]) {
  return [...new Set(candidates.map((candidate) => candidate.word).filter(Boolean))];
}

function removeQualityCandidates(words: string[]) {
  const wordSet = new Set(words);
  qualityCandidates.value = qualityCandidates.value.filter((candidate) => !wordSet.has(candidate.word));
  selectedQualityCandidates.value = selectedQualityCandidates.value.filter((candidate) => !wordSet.has(candidate.word));
}

function summarizeCandidates(candidates: VocabularyQualityCandidate[]) {
  return candidates.reduce(
    (summary, candidate) => ({
      count: summary.count + 1,
      occurrences: summary.occurrences + candidate.occurrenceCount,
      songs: summary.songs + candidate.songCount,
    }),
    { count: 0, occurrences: 0, songs: 0 },
  );
}

function cleanupImpactMessage(key: string, candidates: VocabularyQualityCandidate[]) {
  const impact = summarizeCandidates(candidates);
  return t(key, {
    count: impact.count,
    occurrences: impact.occurrences,
    songs: impact.songs,
  });
}

function loadKeptCleanupWords() {
  try {
    const stored = window.localStorage.getItem(KEPT_CLEANUP_WORDS_STORAGE_KEY);
    const parsed: unknown = stored ? JSON.parse(stored) : [];
    return new Set(Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []);
  } catch {
    return new Set<string>();
  }
}

function saveKeptCleanupWords(words: Set<string>) {
  window.localStorage.setItem(KEPT_CLEANUP_WORDS_STORAGE_KEY, JSON.stringify([...words].sort()));
}

async function updateStatus(id: number, status: VocabularyStatus) {
  isMutating.value = true;
  try {
    await store.updateWord(id, status, masteryScoreForStatus(status));
    syncActiveWord();
    Notify.create({ type: 'positive', message: t('vocabularyStatusUpdated') });
  } catch {
    Notify.create({ type: 'negative', message: t('vocabularyStatusUpdateFailed') });
  } finally {
    isMutating.value = false;
  }
}

async function applyBatchStatus() {
  if (!batchStatus.value || selected.value.length === 0) {
    return;
  }
  isMutating.value = true;
  try {
    await store.updateWords(selected.value.map((word) => word.id), batchStatus.value);
    selected.value = [];
    syncActiveWord();
    Notify.create({ type: 'positive', message: t('batchStatusUpdated') });
  } catch {
    Notify.create({ type: 'negative', message: t('batchStatusUpdateFailed') });
  } finally {
    isMutating.value = false;
  }
}

function confirmRemoveOne(word: UserVocabulary) {
  Dialog.create({
    title: t('removeFromVocabulary'),
    message: t('confirmRemoveWord', { word: word.lemma }),
    cancel: true,
    persistent: true,
    ok: { color: 'negative', label: t('remove') },
  }).onOk(() => void removeWords([word.id]));
}

function confirmRemoveSelected() {
  Dialog.create({
    title: t('removeSelected'),
    message: t('confirmRemoveSelectedWords', { count: selected.value.length }),
    cancel: true,
    persistent: true,
    ok: { color: 'negative', label: t('remove') },
  }).onOk(() => void removeWords(selected.value.map((word) => word.id)));
}

async function removeWords(ids: number[]) {
  isMutating.value = true;
  try {
    await store.deleteWords(ids);
    selected.value = [];
    if (activeWord.value && ids.includes(activeWord.value.id)) {
      activeWord.value = null;
      occurrences.value = [];
    }
    Notify.create({ type: 'positive', message: t('vocabularyWordsRemoved') });
  } catch {
    Notify.create({ type: 'negative', message: t('vocabularyWordsRemoveFailed') });
  } finally {
    isMutating.value = false;
  }
}

function syncActiveWord() {
  if (!activeWord.value) {
    return;
  }
  activeWord.value = store.words.find((word: UserVocabulary) => word.id === activeWord.value?.id) ?? activeWord.value;
}

function masteryScoreForStatus(status: VocabularyStatus) {
  switch (status) {
    case VocabularyStatus.NEW:
      return 0;
    case VocabularyStatus.LEARNING:
      return 0.25;
    case VocabularyStatus.MASTERED:
      return 1;
    case VocabularyStatus.IGNORED:
      return 0;
    default:
      return 0;
  }
}

function buildVocabularyCsv(rows: UserVocabulary[]) {
  const header = ['lemma', 'status', 'masteryScore', 'note', 'firstSeenAt', 'lastSeenAt', 'reviewDueAt'];
  const body = rows.map((word) => [
    word.lemma,
    word.status,
    String(word.masteryScore),
    word.note ?? '',
    word.firstSeenAt,
    word.lastSeenAt,
    word.reviewDueAt ?? '',
  ].map(csvCell).join(','));
  return [header.join(','), ...body].join('\n');
}

function csvCell(value: string) {
  return `"${value.replace(/"/g, '""')}"`;
}

function tsvCell(value: string) {
  return value.replace(/\t/g, ' ').replace(/\r?\n/g, ' ');
}

function downloadTextFile(filename: string, content: string, type: string) {
  const blob = new Blob([content], { type });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

function timestampForFilename() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

type VocabularyImportItem = {
  lemma: string;
  status?: VocabularyStatus;
  masteryScore?: number;
  note?: string | null;
};

function parseVocabularyText(text: string, filename: string): VocabularyImportItem[] {
  const delimiter = filename.toLowerCase().endsWith('.tsv') ? '\t' : ',';
  const lines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  if (lines.length === 0) {
    return [];
  }

  const firstLine = lines[0];
  if (!firstLine) {
    return [];
  }
  const firstCells = splitDelimitedLine(firstLine, delimiter).map((cell) => cell.trim().toLowerCase());
  const hasHeader = firstCells.includes('lemma') || firstCells.includes('word');
  const headers = hasHeader ? firstCells : ['lemma', 'status', 'masteryscore', 'note'];
  const dataLines = hasHeader ? lines.slice(1) : lines;

  return dataLines.map((line) => {
    const cells = splitDelimitedLine(line, delimiter);
    const item: VocabularyImportItem = { lemma: valueForColumn(cells, headers, ['lemma', 'word']) };
    const status = normalizeStatus(valueForColumn(cells, headers, ['status']));
    const masteryScore = Number(valueForColumn(cells, headers, ['masteryscore', 'mastery_score']));
    const note = valueForColumn(cells, headers, ['note']);
    if (status) item.status = status;
    if (Number.isFinite(masteryScore)) item.masteryScore = Math.min(1, Math.max(0, masteryScore));
    if (note) item.note = note;
    return item;
  }).filter((item) => item.lemma);
}

function splitDelimitedLine(line: string, delimiter: string) {
  if (delimiter === '\t') {
    return line.split('\t').map((cell) => cell.trim());
  }

  const cells: string[] = [];
  let current = '';
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];
    if (char === '"' && quoted && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === delimiter && !quoted) {
      cells.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }
  cells.push(current.trim());
  return cells;
}

function valueForColumn(cells: string[], headers: string[], keys: string[]) {
  const index = headers.findIndex((header) => keys.includes(header));
  return index >= 0 ? cells[index]?.trim() ?? '' : '';
}

function normalizeStatus(value: string): VocabularyStatus | undefined {
  const normalized = value.trim().toUpperCase();
  const statuses: Record<string, VocabularyStatus> = {
    NEW: VocabularyStatus.NEW,
    LEARNING: VocabularyStatus.LEARNING,
    MASTERED: VocabularyStatus.MASTERED,
    IGNORED: VocabularyStatus.IGNORED,
  };
  return statuses[normalized];
}
</script>

<style lang="scss" scoped>
.my-vocabulary-page {
  color: var(--lv-ink);
  background: var(--lv-paper);
}

.page-head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--lv-line);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 10px;
}

.stat-tile {
  padding: 12px 14px;
  background: var(--lv-surface);
  border: 1px solid var(--lv-line);
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  color: var(--lv-ink-soft);
  font-size: 12px;
}

.batch-select {
  min-width: 160px;
}

.data-actions {
  background: var(--lv-surface);
}

.cleanup-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
}

.cleanup-summary-item {
  padding: 8px 10px;
  background: var(--lv-surface);
  border: 1px solid var(--lv-line);
  border-radius: 8px;
}

.cleanup-summary-value {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.1;
}

.cleanup-summary-label {
  color: var(--lv-ink-soft);
  font-size: 12px;
}

.section-label {
  color: var(--lv-ink);
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.content-splitter {
  min-height: 620px;
}

.detail-pane {
  min-height: 620px;
  border: 1px solid var(--lv-line);
  border-left: 0;
}

.empty-state {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: var(--lv-ink-soft);
}

.word-button {
  padding: 0;
  color: var(--lv-blue);
  font: inherit;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.occurrence-item {
  padding-left: 0;
  padding-right: 0;
}

.lyric-line {
  white-space: pre-wrap;
}

.quality-panel {
  background: var(--lv-surface-solid);
  border: 1px solid var(--lv-line);
  border-radius: 8px;
}

.candidate-example {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
