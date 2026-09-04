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
      case 'NEW':
      default:
        return null;
    }
  }

  async function updateWord(id: number, status: VocabularyStatus) {
    const saved = await UserVocabularyService.updateUserVocabularyWord(id, { status });
    upsert(saved);
    await refreshStats();
    return saved;
  }

  async function deleteWord(id: number) {
    await UserVocabularyService.deleteUserVocabularyWord(id);
    words.value = words.value.filter((item) => item.id !== id);
    await refreshStats();
  }

  async function updateWords(ids: number[], status: VocabularyStatus) {
    const savedWords = await UserVocabularyService.updateUserVocabularyWordsBatch(
      ids.map((id) => ({ id, status })),
    );
    savedWords.forEach(upsert);
    await refreshStats();
    return savedWords;
  }

  async function deleteWords(ids: number[]) {
    await UserVocabularyService.deleteUserVocabularyWordsBatch(ids);
    const idSet = new Set(ids);
    words.value = words.value.filter((item) => !idSet.has(item.id));
    await refreshStats();
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
    updateWords,
    deleteWord,
    deleteWords,
  };
});
