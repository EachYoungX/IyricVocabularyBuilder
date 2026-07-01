import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  UserVocabularyService,
  type UserVocabulary,
  type UserVocabularyReviewItem,
  type UserVocabularyStats,
  type VocabularyStatus,
} from 'src/services/api';

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
    upsert(saved);
    await refreshStats();
    return saved;
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
