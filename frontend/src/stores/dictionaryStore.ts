import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { DictionaryService, type DictionaryEntry } from 'src/services/api';
import { useI18n } from 'vue-i18n';

export const useDictionaryStore = defineStore('dictionary', () => {
  // I18n
  const { t } = useI18n();

  // State
  const dictionaryEntry = ref<DictionaryEntry | null>(null);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);
  let lookupRequestId = 0;

  // Getters
  const getDictionaryEntry = computed(() => dictionaryEntry.value);
  const getIsLoading = computed(() => isLoading.value);
  const getError = computed(() => error.value);

  // Actions
  async function lookupWord(word: string) {
    const requestId = ++lookupRequestId;
    isLoading.value = true;
    error.value = null;
    try {
      const entry: DictionaryEntry = await DictionaryService.lookupDictionaryWord(word);
      if (requestId !== lookupRequestId) return;
      dictionaryEntry.value = entry;
      return entry;
    } catch (err) {
      if (requestId !== lookupRequestId) return;
      error.value = t('lookupWordFailed', { word });
      dictionaryEntry.value = null;
      console.error(`Error looking up word ${word}:`, err);
    } finally {
      if (requestId === lookupRequestId) isLoading.value = false;
    }
  }

  function clearDictionaryEntry() {
    lookupRequestId += 1;
    dictionaryEntry.value = null;
    error.value = null;
  }

  return {
    // State
    dictionaryEntry,
    isLoading,
    error,

    // Getters
    getDictionaryEntry,
    getIsLoading,
    getError,

    // Actions
    lookupWord,
    clearDictionaryEntry,
  };
});
