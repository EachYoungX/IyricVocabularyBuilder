import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { VocabularyService, type WordOccurrence } from 'src/services/api';
import { useDictionaryStore } from './dictionaryStore';
import { loadAppSettings } from 'src/utils/appSettings';

export const useVocabularyStore = defineStore('vocabulary', () => {
  // State
  const words = ref<string[]>([]);
  const selectedWord = ref<string>('');
  const wordOccurrences = ref<WordOccurrence[]>([]);
  const totalPages = ref<number>(0);
  const totalWords = ref<number>(0);
  const isLoading = ref<boolean>(false);
  const currentWordPage = ref<number>(0);
  const wordPageSize = ref<number>(50);
  const wordSearchPrefix = ref<string>('');
  let wordsRequestId = 0;
  let occurrenceRequestId = 0;

  // Getters
  const getSelectedWord = computed(() => selectedWord.value);
  const getWords = computed(() => words.value);
  const getWordOccurrences = computed(() => wordOccurrences.value);
  const getTotalPages = computed(() => totalPages.value);
  const getTotalWords = computed(() => totalWords.value);
  const getIsLoading = computed(() => isLoading.value);

  interface FetchWordsParams {
    page: number;
    size: number;
    prefix?: string | undefined;
  }

  // Actions
  async function fetchWords(params: FetchWordsParams) {
    const requestId = ++wordsRequestId;
    isLoading.value = true;
    try {
      const { page, size, prefix } = params;
      const settings = loadAppSettings();
      const recommendedOnly = settings.lowValueWordHandling !== 'NORMAL';
      const wordPage = await VocabularyService.getWordList(
        prefix,
        page,
        size,
        recommendedOnly,
        settings.lemmaSearch,
      );
      if (requestId !== wordsRequestId) return;
      if (page === 0) {
        words.value = wordPage.content ?? [];
      } else {
        words.value.push(...(wordPage.content ?? []));
      }
      totalPages.value = wordPage.totalPages;
      totalWords.value = wordPage.totalElements;
      currentWordPage.value = wordPage.number;
      wordPageSize.value = wordPage.size;
      wordSearchPrefix.value = prefix || '';
    } catch (error) {
      if (requestId !== wordsRequestId) return;
      console.error('Failed to fetch words:', error);
    } finally {
      if (requestId === wordsRequestId) isLoading.value = false;
    }
  }

  async function fetchWordOccurrences(word: string) {
    const requestId = ++occurrenceRequestId;
    isLoading.value = true;
    try {
      const occurrences: WordOccurrence[] = await VocabularyService.getWordOccurrences(word);
      if (requestId !== occurrenceRequestId) return;
      wordOccurrences.value = occurrences;
      selectedWord.value = word;

      // 同时获取词典定义
      const dictionaryStore = useDictionaryStore();
      await dictionaryStore.lookupWord(word);
    } catch (error) {
      if (requestId !== occurrenceRequestId) return;
      console.error(`Failed to fetch occurrences for word ${word}:`, error);
      wordOccurrences.value = [];
      selectedWord.value = word;
      // 这里可以添加错误处理，比如显示通知
    } finally {
      if (requestId === occurrenceRequestId) isLoading.value = false;
    }
  }

  function selectWord(word: string) {
    selectedWord.value = word;
  }

  function clearSelectedWord() {
    occurrenceRequestId += 1;
    selectedWord.value = '';
    wordOccurrences.value = [];
  }

  return {
    // State
    words,
    selectedWord,
    wordOccurrences,
    totalPages,
    totalWords,
    isLoading,
    currentWordPage,
    wordPageSize,
    wordSearchPrefix,

    // Getters
    getSelectedWord,
    getWords,
    getWordOccurrences,
    getTotalPages,
    getTotalWords,
    getIsLoading,

    // Actions
    fetchWords,
    fetchWordOccurrences,
    selectWord,
    clearSelectedWord,
  };
});
