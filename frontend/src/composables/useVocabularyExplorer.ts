import { onMounted, ref } from 'vue';
import { useDictionaryStore } from 'src/stores/dictionaryStore';
import { useVocabularyStore } from 'src/stores/vocabularyStore';

const PAGE_SIZE = 50;

export function useVocabularyExplorer() {
  const vocabularyStore = useVocabularyStore();
  const dictionaryStore = useDictionaryStore();
  const searchTerm = ref('');
  const currentPage = ref(1);

  onMounted(() => {
    void vocabularyStore.fetchWords({ page: 0, size: PAGE_SIZE });
  });

  function selectWord(word: string) {
    vocabularyStore.selectWord(word);
    void vocabularyStore.fetchWordOccurrences(word);
  }

  function onSearchChange(value: string | null) {
    currentPage.value = 1;
    void vocabularyStore.fetchWords({
      page: 0,
      size: PAGE_SIZE,
      prefix: value || undefined,
    });
  }

  function onPageChange(page: number) {
    void vocabularyStore.fetchWords({
      page: page - 1,
      size: PAGE_SIZE,
      prefix: searchTerm.value || undefined,
    });
  }

  return {
    currentPage,
    dictionaryStore,
    onPageChange,
    onSearchChange,
    searchTerm,
    selectWord,
    vocabularyStore,
  };
}
