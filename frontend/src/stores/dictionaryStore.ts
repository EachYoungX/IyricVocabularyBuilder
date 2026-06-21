import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { type DictionaryEntry } from 'src/services/api';
import { DictionaryService } from 'src/services/DictionaryService';
import { useI18n } from 'vue-i18n';

export const useDictionaryStore = defineStore('dictionary', () => {
  // I18n
  const { t } = useI18n();

  // State
  const dictionaryEntry = ref<DictionaryEntry | null>(null);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);

  // Getters
  const getDictionaryEntry = computed(() => dictionaryEntry.value);
  const getIsLoading = computed(() => isLoading.value);
  const getError = computed(() => error.value);

  // Actions
  async function lookupWord(word: string) {
    isLoading.value = true;
    error.value = null;
    try {
      const entry: DictionaryEntry = await DictionaryService.lookupDictionaryWord(word);
      dictionaryEntry.value = entry;
      return entry;
    } catch (err) {
      error.value = t('lookupWordFailed', { word });
      dictionaryEntry.value = null;
      console.error(`Error looking up word ${word}:`, err);
    } finally {
      isLoading.value = false;
    }
  }

  function clearDictionaryEntry() {
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
