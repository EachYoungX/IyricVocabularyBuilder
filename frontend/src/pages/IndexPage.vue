<template>
  <q-page class="window-height column no-wrap overflow-hidden q-pa-sm-md">
    <div class="page-masthead col-auto q-mb-md">
      <div>
        <div class="masthead-kicker">{{ t('englishLearningWorkspace') }}</div>
        <div class="masthead-title serif-display">{{ t('totalVocabulary') }}</div>
      </div>
      <div class="masthead-accent" aria-hidden="true"></div>
    </div>

    <!-- 主体内容区域 -->
    <div class="col overflow-hidden">
      <q-splitter
        v-model="splitterModel"
        class="fit desktop-splitter vocabulary-splitter"
        unit="%"
        :horizontal="$q.screen.lt.md"
        :limits="[20, 80]"
      >
        <!-- 左侧词库列表 -->
        <template v-slot:before>
          <div class="column fit no-wrap">
            <!-- 顶部固定区域：统计信息和搜索框 -->
            <div class="col-auto vocabulary-list-header q-pa-sm">
              <div class="row items-center justify-between q-mb-sm">
                <div class="text-caption text-grey">
                  {{ contentMode === 'words' ? t('totalWords', { count: vocabularyStore.getTotalWords }) : t('totalPhrases', { count: phraseTotal }) }}
                </div>
                <q-btn-toggle
                  v-model="contentMode"
                  dense
                  unelevated
                  no-caps
                  toggle-color="primary"
                  :options="contentModeOptions"
                  @update:model-value="handleContentModeChange"
                />
              </div>
              <q-card flat bordered class="q-pa-sm q-mb-sm">
                <div class="row items-center justify-between q-mb-xs">
                  <div class="text-subtitle2">{{ t('personalVocabulary') }}</div>
                  <SemanticChip tone="count">
                    {{ userVocabularyStore.getStats?.totalCount ?? 0 }}
                  </SemanticChip>
                </div>
                <div class="row q-col-gutter-xs text-caption">
                  <div class="col-6">{{ t('newWordsCount', { count: userVocabularyStore.getStats?.newCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('learningWordsCount', { count: userVocabularyStore.getStats?.learningCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('masteredWordsCount', { count: userVocabularyStore.getStats?.masteredCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('dueReviewCount', { count: userVocabularyStore.getStats?.dueReviewCount ?? 0 }) }}</div>
                </div>
              </q-card>
              <q-card v-if="userVocabularyStore.getReviewQueue.length" flat bordered class="q-pa-sm q-mb-sm">
                <div class="text-subtitle2 q-mb-xs">{{ t('reviewQueue') }}</div>
                <q-list dense>
                  <q-item v-for="item in userVocabularyStore.getReviewQueue" :key="item.id" clickable @click="selectWord(item.lemma)">
                    <q-item-section>
                      <q-item-label>{{ item.lemma }}</q-item-label>
                      <q-item-label caption>{{ formatVocabularyStatus(item.status) }}</q-item-label>
                    </q-item-section>
                  </q-item>
                </q-list>
              </q-card>

              <q-input dense outlined v-model="searchTerm" :label="t('searchVocabularyPlaceholder')" clearable
                @update:model-value="onSearchInput" class="q-mb-sm" />
            </div>

            <!-- 中间可滚动区域：单词或短语列表 -->
            <div class="vocabulary-list-scroll col overflow-auto q-px-md">
              <q-list v-if="contentMode === 'words'" bordered separator class="word-list">
                <q-item v-for="word in vocabularyStore.getWords" :key="word" clickable v-ripple
                  :active="word === vocabularyStore.getSelectedWord" @click="selectWord(word)">
                  <q-item-section>
                    <q-item-label>{{ word }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
              <q-list v-else bordered separator class="word-list phrase-list">
                <q-item v-for="phrase in phraseRows" :key="phrase.id" clickable v-ripple
                  :active="phrase.id === selectedPhrase?.id" @click="selectPhrase(phrase)">
                  <q-item-section>
                    <q-item-label>{{ phrase.sourcePattern || phrase.canonicalPattern }}</q-item-label>
                    <q-item-label v-if="phrase.definitionZh || phrase.definitionEn" caption>
                      {{ phrase.definitionZh || phrase.definitionEn }}
                    </q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
              <div v-if="contentMode === 'phrases' && !phraseLoading && phraseRows.length === 0" class="q-pa-md text-center text-grey">
                {{ t('noData') }}
              </div>
              <q-inner-loading :showing="contentMode === 'phrases' && phraseLoading">
                <q-spinner color="primary" size="2em" />
              </q-inner-loading>
            </div>

            <!-- 底部固定区域：分页控件 -->
            <div class="col-auto q-pa-md pagination-container">
              <div class="flex justify-center">
                <q-pagination v-model="currentPage"
                  :max="contentMode === 'words' ? vocabularyStore.getTotalPages : phraseTotalPages"
                  :max-pages="paginationMaxPages" boundary-numbers @update:model-value="handlePageChange" />
              </div>
            </div>
          </div>
        </template>

        <!-- 右侧详情 -->
        <template v-slot:after>
          <div class="fit">
            <q-splitter v-model="horizontalSplitter" horizontal class="fit">
              <!-- 上部: Occurrences 面板 -->
              <template v-slot:before>
                <div class="column fit no-wrap">
                  <div class="detail-panel q-pa-md col overflow-auto">
                    <div class="text-h6 q-mb-md">{{ t('occurrencePositions') }}</div>
                    <div v-if="isOccurrenceLoading" class="text-center">
                      <q-spinner color="primary" size="3em" />
                    </div>
                    <div v-else-if="phraseOccurrenceError" class="text-negative">{{ phraseOccurrenceError }}</div>
                    <q-list v-else-if="showLyricContext && activeOccurrences.length" bordered separator class="occurrence-list">
                      <q-item v-for="(occurrence, index) in activeOccurrences" :key="`${occurrence.songId}-${occurrence.lyricLineId}-${index}`" clickable @click="openOccurrenceSong(occurrence.songId)">
                        <q-item-section top>
                          <q-item-label caption>
                            {{ occurrence.songTitle }}<span v-if="occurrence.songArtist"> · {{ occurrence.songArtist }}</span>
                            <SemanticChip v-if="contentMode === 'words' && showLowValueMarker(occurrence.learningScore)" tone="excluded" class="q-ml-xs">
                              {{ t('excludedVocabularyMarker') }}
                            </SemanticChip>
                          </q-item-label>
                          <q-item-label>
                            <template v-for="(segment, segmentIndex) in lyricLineSegments(occurrence)" :key="segmentIndex">
                              <strong v-if="segment.highlighted" class="phrase-hit">{{ segment.text }}</strong>
                              <template v-else>{{ segment.text }}</template>
                            </template>
                          </q-item-label>
                        </q-item-section>
                      </q-item>
                    </q-list>
                    <div v-else-if="!showLyricContext" class="text-center text-grey">
                      {{ t('lyricContextHiddenBySettings') }}
                    </div>
                    <div v-else-if="hasActiveSelection" class="text-center text-grey">{{ t('noOccurrencesFound') }}</div>
                    <div v-else class="text-center text-grey">
                      {{ contentMode === 'words' ? t('selectWordToViewOccurrences') : t('selectPhraseToViewOccurrences') }}
                    </div>
                  </div>
                </div>
              </template>

              <!-- 下部: Dictionary 面板 -->
              <template v-slot:after>
                <div class="column fit no-wrap">
                  <div class="detail-panel q-pa-md col overflow-auto">
                    <div class="text-h6 q-mb-md">{{ contentMode === 'words' ? t('dictionaryDefinition') : t('phraseDefinition') }}</div>
                    <template v-if="contentMode === 'phrases'">
                      <div v-if="selectedPhrase" class="dictionary-entry">
                        <div class="text-h5 dictionary-word">{{ selectedPhrase.sourcePattern || selectedPhrase.canonicalPattern }}</div>
                        <div v-if="selectedPhrase.canonicalPattern !== selectedPhrase.sourcePattern" class="text-caption text-grey q-mt-xs">
                          {{ selectedPhrase.canonicalPattern }}
                        </div>
                        <div class="q-mt-sm">
                          <SemanticChip>{{ selectedPhrase.phraseType }}</SemanticChip>
                        </div>
                        <div v-if="selectedPhrase.definitionEn" class="q-mt-md text-body1">{{ selectedPhrase.definitionEn }}</div>
                        <div v-if="selectedPhrase.definitionZh" class="q-mt-sm text-body2 text-grey">{{ selectedPhrase.definitionZh }}</div>
                        <div v-if="selectedPhrase.usageNoteZh" class="q-mt-sm text-body2 text-grey">{{ selectedPhrase.usageNoteZh }}</div>
                        <q-separator class="q-my-md" />
                        <div class="text-caption text-grey">{{ t('phraseOccurrenceCount', { count: phraseOccurrences.length }) }}</div>
                      </div>
                      <div v-else class="text-center text-grey">{{ t('selectPhraseToViewDefinition') }}</div>
                    </template>
                    <template v-else>
                    <div v-if="dictionaryStore.getIsLoading" class="text-center">
                      <q-spinner color="primary" size="3em" />
                      <div class="q-mt-sm">{{ t('lookingUpDictionary') }}</div>
                    </div>
                    <div v-else-if="dictionaryStore.getDictionaryEntry" class="dictionary-entry">
                      <div class="text-h5 dictionary-word">{{ dictionaryStore.getDictionaryEntry.word }}</div>
                      <div v-if="showPhoneticAndPos && dictionaryStore.getDictionaryEntry.phonetic" class="text-subtitle1 text-grey">
                        {{ dictionaryStore.getDictionaryEntry.phonetic }}
                      </div>
                      <div v-if="showPhoneticAndPos" class="q-mt-sm">
                        <SemanticChip v-if="dictionaryStore.getDictionaryEntry.pos">
                          {{ dictionaryStore.getDictionaryEntry.pos }}
                        </SemanticChip>
                      </div>
                      <div v-if="showEnglishDefinition" class="q-mt-sm">
                        <div class="text-body1">{{ dictionaryStore.getDictionaryEntry.definition }}</div>
                      </div>
                      <div v-if="showChineseDefinition && dictionaryStore.getDictionaryEntry.translation" class="q-mt-sm text-body2 text-grey">
                        {{ dictionaryStore.getDictionaryEntry.translation }}
                      </div>
                      <q-separator class="q-my-md" />
                      <div class="word-status-row">
                        <div class="word-status-left">
                          <SemanticChip v-if="selectedUserWord" tone="status" class="status-chip">
                            {{ formatVocabularyStatus(selectedUserWord.status) }}
                          </SemanticChip>
                          <SemanticChip v-else class="status-chip">
                            {{ t('notInPersonalVocabulary') }}
                          </SemanticChip>
                        </div>
                        <div class="word-status-action">
                          <q-btn v-if="!selectedUserWord" color="primary" outline no-caps icon="bookmark_add" :loading="personalActionLoading" @click="addSelectedWord">
                            {{ t('addToPersonalVocabulary') }}
                          </q-btn>
                          <q-btn-dropdown v-else color="primary" no-caps icon="school" unelevated :loading="personalActionLoading" :label="t('updateLearningStatus')">
                            <q-list>
                              <q-item v-for="status in learningStatuses" :key="status" clickable v-close-popup @click="updateSelectedWordStatus(status)">
                                <q-item-section>{{ formatVocabularyStatus(status) }}</q-item-section>
                              </q-item>
                            </q-list>
                          </q-btn-dropdown>
                        </div>
                      </div>
                      <div class="learning-value-row q-mt-md">
                        <SemanticChip :tone="selectedWordIsLowValue ? 'excluded' : 'status'">
                          {{ selectedWordIsLowValue ? t('excludedVocabularyMarker') : t('normalVocabularyMarker') }}
                        </SemanticChip>
                        <div class="learning-value-actions">
                          <q-btn
                            outline
                            no-caps
                            icon="trending_up"
                            :loading="learningValueLoading"
                            :disable="!vocabularyStore.getSelectedWord || !selectedWordIsLowValue"
                            :label="t('restoreExcludedVocabulary')"
                            @click="updateSelectedWordLearningValue(true)"
                          />
                          <q-btn
                            outline
                            no-caps
                            color="warning"
                            icon="block"
                            :loading="learningValueLoading"
                            :disable="!vocabularyStore.getSelectedWord || selectedWordIsLowValue"
                            :label="t('excludeVocabulary')"
                            @click="updateSelectedWordLearningValue(false)"
                          />
                        </div>
                      </div>
                    </div>
                    <div v-else-if="dictionaryStore.getError" class="text-center text-negative">
                      {{ dictionaryStore.getError }}
                    </div>
                    <div v-else-if="vocabularyStore.getSelectedWord" class="text-center text-grey">
                      {{ t('noDictionaryEntryFound', { word: vocabularyStore.getSelectedWord }) }}
                    </div>
                    <div v-else class="text-center text-grey">
                      {{ t('selectWordToViewDefinition') }}
                    </div>
                    </template>
                  </div>
                </div>
              </template>
            </q-splitter>
          </div>
        </template>
      </q-splitter>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { useVocabularyExplorer } from 'src/composables/useVocabularyExplorer';
import { PhrasesService, VocabularyService, VocabularyStatus, type DictionaryPhrase, type WordOccurrence } from 'src/services/api';
import { useUserVocabularyStore } from 'src/stores/userVocabularyStore';
import { loadAppSettings, type AppSettings } from 'src/utils/appSettings';
import SemanticChip from 'src/components/SemanticChip.vue';
import { useRouter } from 'vue-router';

const { t } = useI18n();
const $q = useQuasar();
const router = useRouter();
const {
  currentPage,
  dictionaryStore,
  onPageChange,
  onSearchChange,
  searchTerm,
  selectWord,
  vocabularyStore,
} = useVocabularyExplorer();

const userVocabularyStore = useUserVocabularyStore();
const splitterModel = ref<number>(30);
const horizontalSplitter = ref<number>(50);
type VocabularyMode = 'words' | 'phrases';
type ExplorerOccurrence = {
  songId?: number;
  songTitle: string;
  songArtist?: string | null;
  lyricLineId?: number | null;
  lyricLine: string;
  learningScore?: number;
  surfacePhrase?: string;
};
type LyricLineSegment = { text: string; highlighted: boolean };
const contentMode = ref<VocabularyMode>('words');
const phraseRows = ref<DictionaryPhrase[]>([]);
const phraseTotal = ref(0);
const phraseTotalPages = ref(0);
const phraseLoading = ref(false);
const selectedPhrase = ref<DictionaryPhrase | null>(null);
const phraseOccurrences = ref<ExplorerOccurrence[]>([]);
const phraseOccurrenceLoading = ref(false);
const phraseOccurrenceError = ref('');
const personalActionLoading = ref(false);
const learningValueLoading = ref(false);
const appSettings = ref<AppSettings>(loadAppSettings());

const lyricLineSegments = (occurrence: ExplorerOccurrence): LyricLineSegment[] => {
  const line = occurrence.lyricLine || '';
  const phrase = occurrence.surfacePhrase?.trim() || '';
  if (!line || !phrase) return [{ text: line, highlighted: false }];

  const start = line.toLocaleLowerCase().indexOf(phrase.toLocaleLowerCase());
  if (start < 0) return [{ text: line, highlighted: false }];

  return [
    { text: line.slice(0, start), highlighted: false },
    { text: line.slice(start, start + phrase.length), highlighted: true },
    { text: line.slice(start + phrase.length), highlighted: false },
  ].filter((segment) => segment.text.length > 0);
};
const paginationMaxPages = computed(() => {
  if ($q.screen.lt.sm) return 3;
  if ($q.screen.lt.md || splitterModel.value < 28) return 4;
  return 5;
});
const contentModeOptions = computed(() => [
  { label: t('wordsTab'), value: 'words' },
  { label: t('phrasesTab'), value: 'phrases' },
]);
const learningStatuses = [
  VocabularyStatus.NEW,
  VocabularyStatus.LEARNING,
  VocabularyStatus.MASTERED,
  VocabularyStatus.IGNORED,
];

const selectedUserWord = computed(() => userVocabularyStore.findByLemma(vocabularyStore.getSelectedWord));
const showLyricContext = computed(() => appSettings.value.dictionaryDisplay.includes('LYRIC_CONTEXT'));
const showPhoneticAndPos = computed(() => appSettings.value.dictionaryDisplay.includes('PHONETIC_POS'));
const showEnglishDefinition = computed(() =>
  appSettings.value.dictionaryDisplay.some((item) => item === 'BRIEF' || item === 'FULL')
  && (appSettings.value.definitionLanguage === 'EN' || appSettings.value.definitionLanguage === 'BILINGUAL'),
);
const showChineseDefinition = computed(() =>
  appSettings.value.definitionLanguage === 'ZH' || appSettings.value.definitionLanguage === 'BILINGUAL',
);
const selectedWordLearningScore = computed(() => {
  const scores = vocabularyStore.getWordOccurrences
    .map((occurrence: WordOccurrence) => occurrence.learningScore)
    .filter((score: WordOccurrence['learningScore']): score is number => typeof score === 'number');
  return scores.length > 0 ? Math.max(...scores) : 1;
});
const selectedWordIsLowValue = computed(() => selectedWordLearningScore.value < 0.5);
const activeOccurrences = computed<ExplorerOccurrence[]>(() =>
  contentMode.value === 'words' ? vocabularyStore.getWordOccurrences : phraseOccurrences.value,
);
const isOccurrenceLoading = computed(() =>
  contentMode.value === 'words' ? vocabularyStore.getIsLoading : phraseOccurrenceLoading.value,
);
const hasActiveSelection = computed(() =>
  contentMode.value === 'words' ? Boolean(vocabularyStore.getSelectedWord) : Boolean(selectedPhrase.value),
);

onMounted(() => {
  appSettings.value = loadAppSettings();
  void userVocabularyStore.fetchDashboard();
});

function formatVocabularyStatus(status: VocabularyStatus | undefined) {
  return status ? t(`vocabularyStatuses.${status}`) : t('unknown');
}

function showLowValueMarker(score: WordOccurrence['learningScore']) {
  return appSettings.value.lowValueWordHandling === 'QUERY_ONLY'
    && typeof score === 'number'
    && score < 0.5;
}

async function addSelectedWord() {
  if (!vocabularyStore.getSelectedWord) return;
  personalActionLoading.value = true;
  try {
    await userVocabularyStore.addWord(vocabularyStore.getSelectedWord);
    $q.notify({ type: 'positive', position: 'top-right', message: t('vocabularyAddedSuccessfully') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('vocabularyAddFailed') });
  } finally {
    personalActionLoading.value = false;
  }
}

async function updateSelectedWordStatus(status: VocabularyStatus) {
  if (!selectedUserWord.value?.id) return;
  personalActionLoading.value = true;
  try {
    await userVocabularyStore.updateWord(
      selectedUserWord.value.id,
      status,
      masteryScoreForStatus(status),
    );
    $q.notify({ type: 'positive', position: 'top-right', message: t('vocabularyStatusUpdated') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('vocabularyStatusUpdateFailed') });
  } finally {
    personalActionLoading.value = false;
  }
}

function onSearchInput(value: string | number | null) {
  const query = value === null ? '' : String(value);
  if (contentMode.value === 'phrases') {
    currentPage.value = 1;
    void fetchPhrases(0, query);
    return;
  }
  onSearchChange(query || null);
}

function handleContentModeChange(mode: VocabularyMode) {
  currentPage.value = 1;
  selectedPhrase.value = null;
  phraseOccurrences.value = [];
  phraseOccurrenceError.value = '';
  vocabularyStore.clearSelectedWord();
  dictionaryStore.clearDictionaryEntry();
  if (mode === 'phrases') {
    void fetchPhrases(0, searchTerm.value);
  } else {
    onSearchChange(searchTerm.value || null);
  }
}

async function fetchPhrases(page: number, query: string) {
  phraseLoading.value = true;
  try {
    const result = await PhrasesService.listPhrases(query.trim() || undefined, page, 50);
    phraseRows.value = result.content ?? [];
    phraseTotal.value = result.totalElements;
    phraseTotalPages.value = result.totalPages;
    currentPage.value = result.number + 1;
  } catch (error) {
    phraseRows.value = [];
    phraseTotal.value = 0;
    phraseTotalPages.value = 0;
    console.error('Failed to fetch phrases:', error);
  } finally {
    phraseLoading.value = false;
  }
}

function handlePageChange(page: number) {
  if (contentMode.value === 'phrases') {
    void fetchPhrases(page - 1, searchTerm.value);
    return;
  }
  onPageChange(page);
}

function selectPhrase(phrase: DictionaryPhrase) {
  if (phrase.id == null) return;
  selectedPhrase.value = phrase;
  phraseOccurrences.value = [];
  phraseOccurrenceError.value = '';
  phraseOccurrenceLoading.value = true;
  void PhrasesService.getPhraseOccurrences(phrase.id)
    .then((occurrences) => {
      phraseOccurrences.value = occurrences;
    })
    .catch((error) => {
      phraseOccurrenceError.value = t('loadOccurrencesFailed');
      console.error(`Failed to fetch phrase occurrences for ${phrase.id}:`, error);
    })
    .finally(() => {
      phraseOccurrenceLoading.value = false;
    });
}

async function updateSelectedWordLearningValue(recommended: boolean) {
  if (!vocabularyStore.getSelectedWord) return;
  if (!recommended && selectedUserWord.value) {
    const confirmed = await new Promise<boolean>((resolve) => {
      $q.dialog({
        title: t('excludePersonalVocabularyTitle'),
        message: t('excludePersonalVocabularyMessage'),
        cancel: true,
        ok: t('continueExcludeVocabulary'),
      }).onOk(() => resolve(true)).onCancel(() => resolve(false));
    });
    if (!confirmed) return;
  }
  learningValueLoading.value = true;
  try {
    const selectedWord = vocabularyStore.getSelectedWord;
    await VocabularyService.updateVocabularyLearningValue(selectedWord, { recommended });
    await vocabularyStore.fetchWordOccurrences(selectedWord);
    currentPage.value = 1;
    await vocabularyStore.fetchWords({
      page: 0,
      size: vocabularyStore.wordPageSize,
      prefix: vocabularyStore.wordSearchPrefix || undefined,
    });
    $q.notify({ type: 'positive', position: 'top-right', message: t('learningValueUpdated') });
  } catch {
    $q.notify({ type: 'negative', position: 'top-right', message: t('learningValueUpdateFailed') });
  } finally {
    learningValueLoading.value = false;
  }
}

function openOccurrenceSong(songId?: number) {
  if (songId) void router.push({ path: '/songs', query: { songId: String(songId) } });
}

function masteryScoreForStatus(status: VocabularyStatus) {
  switch (status) {
    case VocabularyStatus.NEW:
      return 0;
    case VocabularyStatus.LEARNING:
      return 0.35;
    case VocabularyStatus.MASTERED:
      return 1;
    case VocabularyStatus.FAMILIAR:
    case VocabularyStatus.BOOKMARK_ONLY:
    case VocabularyStatus.IGNORED:
      return 0;
  }
}
</script>

<style lang="scss" scoped>
.page-masthead {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.masthead-kicker {
  color: var(--lv-ink-soft);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.masthead-title {
  color: var(--lv-ink);
  font-size: clamp(28px, 4vw, 44px);
  font-weight: 700;
  line-height: 0.95;
}

.masthead-accent {
  width: min(28vw, 180px);
  height: 2px;
  margin-bottom: 9px;
  background: linear-gradient(90deg, transparent, var(--lv-sand), var(--lv-blue));
  border-radius: 999px;
}

.vocabulary-list-header {
  padding-block: 10px 4px;
}

.vocabulary-list-scroll {
  min-height: 0;
}

.vocabulary-list-header :deep(.q-btn-toggle) {
  border: 1px solid var(--lv-line);
  border-radius: 8px;
  overflow: hidden;
}

.vocabulary-list-header :deep(.q-btn-toggle .q-btn) {
  min-height: 32px;
  padding-inline: 12px;
}

.phrase-list :deep(.q-item-label--caption) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phrase-hit {
  color: inherit;
  font-weight: 700;
}

/* 响应式内边距 */
@media (max-width: 600px) {
  .q-pa-sm-md {
    padding: 8px;
  }

  .page-masthead {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .masthead-accent {
    width: 100%;
    margin-bottom: 0;
  }
}

@media (min-width: 601px) {
  .q-pa-sm-md {
    padding: 16px;
  }
}

/* 桌面端：整体边框 + 圆角 + 内部面板分隔 */
@media (min-width: 768px) {
  .desktop-splitter {
    border: 1px solid var(--lv-line);
    border-radius: var(--lv-radius-md);
    background: var(--lv-surface-solid);
    overflow: hidden;
  }

  /* 隐藏默认分隔线，用边框模拟 */
  :deep(.q-splitter__separator) {
    background-color: transparent;
    width: 1px;
  }

  :deep(.q-splitter__separator-area) {
    cursor: col-resize;
    z-index: 10;
    background-color: transparent;
    &:hover {
      background-color: rgba(27, 60, 83, 0.06);
    }
  }

  /* 左侧面板右边框 */
  :deep(.q-splitter__panel:first-child) {
    border-right: 1px solid var(--lv-line);
  }
}

/* 移动端：水平分割 */
@media (max-width: 767px) {
  .desktop-splitter {
    border: 1px solid var(--lv-line);
    border-radius: var(--lv-radius-md);
    background: var(--lv-surface-solid);
    overflow: hidden;
  }

  :deep(.q-splitter__separator) {
    background-color: var(--lv-line);
    height: 8px;
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      height: 4px;
      width: 40px;
      background-color: var(--lv-line-strong);
      border-radius: 2px;
      opacity: 0.7;
    }
  }

  :deep(.q-splitter__separator-area) {
    cursor: row-resize;
  }
}

@media (max-width: 1023px) and (orientation: portrait) {
  .vocabulary-splitter {
    min-height: 0;
  }

  :deep(.vocabulary-splitter > .q-splitter__panel:first-child) {
    min-height: 280px;
  }
}

@media (max-width: 1023px) and (orientation: landscape) {
  .page-masthead {
    margin-bottom: 8px;
  }

  .masthead-title {
    font-size: 28px;
  }
}

/* 保持你原来的 q-list 圆角 */
.q-list {
  border-radius: var(--lv-radius-sm);
  overflow: hidden;
}

/* 修复布局问题 - 关键修复 */
.word-list,
.occurrence-list,
.dictionary-entry {
  min-height: 0; /* 允许内容收缩 */
}

.detail-panel {
  padding-inline: 18px;
  border: 1px solid var(--lv-line);
  border-radius: var(--lv-radius-md);
  margin: 12px;
  background: var(--lv-surface);
}

.occurrence-list,
.dictionary-entry {
  width: 100%;
}

.occurrence-list {
  border-color: var(--lv-line);
  background: var(--lv-surface-solid);
}

.occurrence-list :deep(.q-item) {
  padding-inline: 14px;
}

.dictionary-entry {
  padding-inline: 14px;
}

.dictionary-word {
  line-height: 1.1;
}

.word-status-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--lv-line);
  border-bottom: 1px solid var(--lv-line);
}

.word-status-left,
.word-status-action {
  min-width: 0;
}

.word-status-action :deep(.q-btn),
.word-status-action :deep(.q-btn-dropdown) {
  font-size: 13px;
}

.learning-value-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.learning-value-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.status-chip {
  min-height: 28px;
  padding-inline: 12px;
  font-size: 13px;
}

@media (max-width: 600px) {
  .word-status-action,
  .learning-value-actions {
    width: 100%;
  }

  .word-status-action :deep(.q-btn),
  .learning-value-actions :deep(.q-btn) {
    width: 100%;
  }
}

.pagination-container {
  flex-shrink: 0; /* 防止分页被压缩 */
  border-top: 1px solid var(--lv-line); /* 添加分隔线 */
  padding: 12px 8px;
  background-color: var(--lv-surface-solid); /* 确保背景色一致 */
}

.pagination-container :deep(.q-pagination) {
  max-width: 100%;
  justify-content: center;
  row-gap: 2px;
}

/* 确保分割器面板正确填充 */
:deep(.q-splitter__panel) {
  display: flex;
  flex-direction: column;

  > .column {
    height: 100%;
  }
}

/* 确保内容区域正确滚动 */
.overflow-auto {
  min-height: 0; /* 关键：允许flex子项收缩 */
}

/* 为左侧面板添加明确的高度分配 */
:deep(.q-splitter__panel:first-child) .column {
  > .col-auto:first-child {
    /* 顶部固定区域 */
    flex-shrink: 0;
  }

  > .col {
    /* 中间可滚动区域 */
    flex: 1 1 auto;
    min-height: 0;
  }

  > .col-auto:last-child {
    /* 底部固定区域 */
    flex-shrink: 0;
  }
}

/* 确保单词列表有最小高度 */
.word-list {
  min-height: 200px; /* 确保即使单词很少也有一定高度 */
}
</style>
