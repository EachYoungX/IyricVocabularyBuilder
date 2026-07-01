<template>
  <q-page class="window-height column no-wrap overflow-hidden q-pa-sm-md">
    <!-- 主体内容区域 -->
    <div class="col overflow-hidden">
      <q-splitter v-model="splitterModel" class="fit desktop-splitter" unit="%" :limits="[20, 80]">
        <!-- 左侧单词列表 -->
        <template v-slot:before>
          <div class="column fit no-wrap">
            <!-- 顶部固定区域：统计信息和搜索框 -->
            <div class="col-auto q-pa-md">
              <div class="text-caption text-grey q-mb-md">
                {{ t('totalWords', { count: vocabularyStore.getTotalWords }) }}
              </div>
              <q-card flat bordered class="q-pa-sm q-mb-md">
                <div class="row items-center justify-between q-mb-xs">
                  <div class="text-subtitle2">{{ t('personalVocabulary') }}</div>
                  <q-chip dense color="primary" text-color="white">
                    {{ userVocabularyStore.getStats?.totalCount ?? 0 }}
                  </q-chip>
                </div>
                <div class="row q-col-gutter-xs text-caption">
                  <div class="col-6">{{ t('newWordsCount', { count: userVocabularyStore.getStats?.newCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('learningWordsCount', { count: userVocabularyStore.getStats?.learningCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('masteredWordsCount', { count: userVocabularyStore.getStats?.masteredCount ?? 0 }) }}</div>
                  <div class="col-6">{{ t('dueReviewCount', { count: userVocabularyStore.getStats?.dueReviewCount ?? 0 }) }}</div>
                </div>
              </q-card>
              <q-card v-if="userVocabularyStore.getReviewQueue.length" flat bordered class="q-pa-sm q-mb-md">
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

              <q-input dense outlined v-model="searchTerm" :label="t('searchPlaceholder')" clearable
                @update:model-value="onSearchChange as (value: string | number | null) => void" class="q-mb-md" />
            </div>

            <!-- 中间可滚动区域：单词列表 -->
            <div class="col overflow-auto q-px-md">
              <q-list bordered separator class="word-list">
                <q-item v-for="word in vocabularyStore.getWords" :key="word" clickable v-ripple
                  :active="word === vocabularyStore.getSelectedWord" @click="selectWord(word)">
                  <q-item-section>
                    <q-item-label>{{ word }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </div>

            <!-- 底部固定区域：分页控件 -->
            <div class="col-auto q-pa-md pagination-container">
              <div class="flex justify-center">
                <q-pagination v-model="currentPage" :max="vocabularyStore.getTotalPages" :max-pages="6" boundary-numbers
                  @update:model-value="onPageChange" />
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
                  <div class="q-pa-md col overflow-auto">
                    <div class="text-h6 q-mb-md">{{ t('occurrencePositions') }}</div>
                    <div v-if="vocabularyStore.getIsLoading" class="text-center">
                      <q-spinner color="primary" size="3em" />
                    </div>
                    <q-list v-else bordered separator class="occurrence-list">
                      <q-item v-for="(occurrence, index) in vocabularyStore.getWordOccurrences" :key="index">
                        <q-item-section top>
                          <q-item-label caption>{{ occurrence.songTitle }}</q-item-label>
                          <q-item-label>{{ occurrence.lyricLine }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </q-list>
                  </div>
                </div>
              </template>

              <!-- 下部: Dictionary 面板 -->
              <template v-slot:after>
                <div class="column fit no-wrap">
                  <div class="q-pa-md col overflow-auto">
                    <div class="text-h6 q-mb-md">{{ t('dictionaryDefinition') }}</div>
                    <div v-if="dictionaryStore.getIsLoading" class="text-center">
                      <q-spinner color="primary" size="3em" />
                      <div class="q-mt-sm">{{ t('lookingUpDictionary') }}</div>
                    </div>
                    <div v-else-if="dictionaryStore.getDictionaryEntry" class="dictionary-entry">
                      <div class="text-h5">{{ dictionaryStore.getDictionaryEntry.word }}</div>
                      <div v-if="dictionaryStore.getDictionaryEntry.phonetic" class="text-subtitle1 text-grey">
                        {{ dictionaryStore.getDictionaryEntry.phonetic }}
                      </div>
                      <div class="q-mt-sm">
                        <q-chip v-if="dictionaryStore.getDictionaryEntry.pos" dense>
                          {{ dictionaryStore.getDictionaryEntry.pos }}
                        </q-chip>
                      </div>
                      <div class="q-mt-sm">
                        <div class="text-body1">{{ dictionaryStore.getDictionaryEntry.definition }}</div>
                      </div>
                      <div v-if="dictionaryStore.getDictionaryEntry.translation" class="q-mt-sm text-body2 text-grey">
                        {{ dictionaryStore.getDictionaryEntry.translation }}
                      </div>
                      <q-separator class="q-my-md" />
                      <div class="row items-center q-col-gutter-sm">
                        <div class="col-auto">
                          <q-chip v-if="selectedUserWord" color="secondary" text-color="white">
                            {{ formatVocabularyStatus(selectedUserWord.status) }}
                          </q-chip>
                          <q-chip v-else>
                            {{ t('notInPersonalVocabulary') }}
                          </q-chip>
                        </div>
                        <div class="col-auto">
                          <q-btn v-if="!selectedUserWord" color="primary" :loading="personalActionLoading" @click="addSelectedWord">
                            {{ t('addToPersonalVocabulary') }}
                          </q-btn>
                          <q-btn-dropdown v-else color="primary" :loading="personalActionLoading" :label="t('updateLearningStatus')">
                            <q-list>
                              <q-item v-for="status in learningStatuses" :key="status" clickable v-close-popup @click="updateSelectedWordStatus(status)">
                                <q-item-section>{{ formatVocabularyStatus(status) }}</q-item-section>
                              </q-item>
                            </q-list>
                          </q-btn-dropdown>
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
import { useI18n } from 'vue-i18n';
import { useVocabularyExplorer } from 'src/composables/useVocabularyExplorer';
import { VocabularyStatus } from 'src/services/api';
import { useUserVocabularyStore } from 'src/stores/userVocabularyStore';

const { t } = useI18n();
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
const personalActionLoading = ref(false);
const learningStatuses = [
  VocabularyStatus.NEW,
  VocabularyStatus.LEARNING,
  VocabularyStatus.FAMILIAR,
  VocabularyStatus.MASTERED,
  VocabularyStatus.IGNORED,
];

const selectedUserWord = computed(() => userVocabularyStore.findByLemma(vocabularyStore.getSelectedWord));

onMounted(() => {
  void userVocabularyStore.fetchDashboard();
});

function formatVocabularyStatus(status: VocabularyStatus | undefined) {
  return status ? t(`vocabularyStatuses.${status}`) : t('unknown');
}

async function addSelectedWord() {
  if (!vocabularyStore.getSelectedWord) return;
  personalActionLoading.value = true;
  try {
    await userVocabularyStore.addWord(vocabularyStore.getSelectedWord);
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
  } finally {
    personalActionLoading.value = false;
  }
}

function masteryScoreForStatus(status: VocabularyStatus) {
  switch (status) {
    case VocabularyStatus.NEW:
      return 0;
    case VocabularyStatus.LEARNING:
      return 0.35;
    case VocabularyStatus.FAMILIAR:
      return 0.7;
    case VocabularyStatus.MASTERED:
      return 1;
    case VocabularyStatus.IGNORED:
      return 0;
  }
}
</script>

<style lang="scss" scoped>
/* 响应式内边距 */
@media (max-width: 600px) {
  .q-pa-sm-md {
    padding: 8px;
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
    border: 1px solid #e0e0e0;
    border-radius: 8px;
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
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  /* 左侧面板右边框 */
  :deep(.q-splitter__panel:first-child) {
    border-right: 1px solid #e0e0e0;
  }
}

/* 移动端：水平分割 */
@media (max-width: 767px) {
  :deep(.q-splitter__separator) {
    background-color: #e0e0e0;
    height: 8px;
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      height: 4px;
      width: 40px;
      background-color: #bdbdbd;
      border-radius: 2px;
      opacity: 0.7;
    }
  }

  :deep(.q-splitter__separator-area) {
    cursor: row-resize;
  }
}

/* 保持你原来的 q-list 圆角 */
.q-list {
  border-radius: 8px;
  overflow: hidden;
}

/* 修复布局问题 - 关键修复 */
.word-list,
.occurrence-list,
.dictionary-entry {
  min-height: 0; /* 允许内容收缩 */
}

.pagination-container {
  flex-shrink: 0; /* 防止分页被压缩 */
  border-top: 1px solid rgba(0, 0, 0, 0.12); /* 添加分隔线 */
  padding-top: 16px;
  background-color: white; /* 确保背景色一致 */
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
