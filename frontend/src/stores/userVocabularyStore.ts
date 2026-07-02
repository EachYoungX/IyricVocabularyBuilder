import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  UserVocabularyService,
  VocabularyStatus,
  type UserVocabulary,
  type UserVocabularyReviewItem,
  type UserVocabularyStats,
} from 'src/services/api';
import { loadAppSettings } from 'src/utils/appSettings';

export const useUserVocabularyStore = defineStore('userVocabulary', () => {
  const words = ref<UserVocabulary[]>([]);
  const stats = ref<UserVocabularyStats | null>(null);
  const reviewQueue = ref<UserVocabularyReviewItem[]>([]);
  const isLoading = ref(false);

  const getWords = computed(() => words.value);
  const getStats = computed(() => stats.value);
  const getReviewQueue = computed(() => reviewQueue.value);
  const getIsLoading = computed(() => isLoading.value);

  const findByLemma = (lemma: string) => words.value.find((word) => word.lemma === lemma);

  async function fetchDashboard() {
    isLoading.value = true;
    try {
      const [wordList, userStats, reviews] = await Promise.all([
        UserVocabularyService.listUserVocabularyWords(),
        UserVocabularyService.getUserVocabularyStats(),
        UserVocabularyService.getUserVocabularyReviewQueue(5),
      ]);
      words.value = wordList;
      stats.value = userStats;
      reviewQueue.value = reviews;
    } catch (error) {
      console.error('Failed to fetch user vocabulary dashboard:', error);
    } finally {
      isLoading.value = false;
    }
  }

  async function addWord(lemma: string) {
    const saved = await UserVocabularyService.addUserVocabularyWord({ lemma });
    const targetStatus = defaultStatusForNewWord();
    const word = targetStatus
      ? await UserVocabularyService.updateUserVocabularyWord(saved.id, {
        status: targetStatus,
        masteryScore: masteryScoreForRequiredStatus(targetStatus),
      })
      : saved;
    upsert(word);
    await refreshStats();
    return word;
  }

  function defaultStatusForNewWord() {
    switch (loadAppSettings().defaultNewWordStatus) {
      case 'LEARNING':
        return VocabularyStatus.LEARNING;
      case 'BOOKMARK_ONLY':
        return VocabularyStatus.BOOKMARK_ONLY;
      case 'NEW':
      default:
        return null;
    }
  }

  async function updateWord(id: number, status: VocabularyStatus, masteryScore?: number) {
    const request = masteryScore === undefined ? { status } : { status, masteryScore };
    const saved = await UserVocabularyService.updateUserVocabularyWord(id, request);
    upsert(saved);
    await refreshStats();
    return saved;
  }

  async function refreshStats() {
    const [userStats, reviews] = await Promise.all([
      UserVocabularyService.getUserVocabularyStats(),
      UserVocabularyService.getUserVocabularyReviewQueue(5),
    ]);
    stats.value = userStats;
    reviewQueue.value = reviews;
  }

  function upsert(word: UserVocabulary) {
    const index = words.value.findIndex((item) => item.id === word.id);
    if (index >= 0) {
      words.value.splice(index, 1, word);
    } else {
      words.value.unshift(word);
    }
  }

  function masteryScoreForStatus(status: VocabularyStatus) {
    switch (status) {
      case VocabularyStatus.NEW:
        return 0;
      case VocabularyStatus.LEARNING:
        return 0.25;
      case VocabularyStatus.FAMILIAR:
        return 0.6;
      case VocabularyStatus.MASTERED:
        return 1;
      case VocabularyStatus.BOOKMARK_ONLY:
      case VocabularyStatus.IGNORED:
        return 0;
      default:
        return undefined;
    }
  }

  function masteryScoreForRequiredStatus(status: VocabularyStatus) {
    return masteryScoreForStatus(status) ?? 0;
  }

  return {
    words,
    stats,
    reviewQueue,
    isLoading,
    getWords,
    getStats,
    getReviewQueue,
    getIsLoading,
    findByLemma,
    fetchDashboard,
    addWord,
    updateWord,
  };
});
