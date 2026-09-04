import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  SongsService,
  type Song,
  type SongSummary,
  type SongImportRequest,
  type ImportTaskResult,
  type SongUpdateRequest,
} from 'src/services/api';
import { Notify } from 'quasar';
import { useI18n } from 'vue-i18n';
import { loadAppSettings } from 'src/utils/appSettings';

export const useSongsStore = defineStore('songs', () => {
  // I18n
  const { t } = useI18n();

  // State
  const songs = ref<SongSummary[]>([]);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);
  const importTask = ref<ImportTaskResult | null>(null);

  // Getters
  const getSongs = computed(() => songs.value);
  const getIsLoading = computed(() => isLoading.value);
  const getError = computed(() => error.value);
  const getImportTask = computed(() => importTask.value);

  // Actions
  async function fetchAllSongs(showSuccessNotify = true) {
    isLoading.value = true;
    error.value = null;
    try {
      const songList: SongSummary[] = await SongsService.getAllSongs();
      songs.value = songList;
      if (showSuccessNotify) {
        Notify.create({
          type: 'positive',
          message: t('songsLoadedSuccessfully', { count: songList.length }),
          position: 'top-right',
        });
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      Notify.create({ type: 'negative', message: t('fetchSongsFailed', { error: errorMessage }), position: 'top-right' });
      console.error('Error fetching songs:', err);
    } finally {
      isLoading.value = false;
    }
  }

  async function importSongs(songsToImport: SongImportRequest[]) {
    isLoading.value = true;
    error.value = null;
    try {
      const result = await SongsService.importSongsAsync(songsToImport, loadAppSettings().autoAddImportedWords);
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      console.error('Error importing songs:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  async function checkImportTaskStatus(taskId: string) {
    isLoading.value = true;
    error.value = null;
    try {
      const taskResult: ImportTaskResult = await SongsService.getImportTaskResult(taskId);
      importTask.value = taskResult;
      return taskResult;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      Notify.create({
        type: 'negative',
        message: t('checkImportTaskStatusFailed', { error: errorMessage }),
        position: 'top-right',
      });
      console.error('Error checking import task status:', err);
    } finally {
      isLoading.value = false;
    }
  }

  async function updateSong(id: number, songUpdate: SongUpdateRequest): Promise<boolean> {
    isLoading.value = true;
    error.value = null;
    try {
      const updatedSong: Song = await SongsService.updateSong(id, songUpdate);
      // 更新歌曲列表中的歌曲
      const index = songs.value.findIndex((song) => song.id === id);
      if (index !== -1) {
        songs.value[index] = {
          id: updatedSong.id,
          title: updatedSong.title,
          artist: updatedSong.artist,
          ...(updatedSong.album !== undefined ? { album: updatedSong.album } : {}),
        };
      }
      Notify.create({
        type: 'positive',
        message: t('songUpdatedSuccessfully', { title: updatedSong.title }),
        position: 'top-right',
      });
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      Notify.create({ type: 'negative', message: t('updateSongFailed', { error: errorMessage }), position: 'top-right' });
      console.error('Error updating song:', err);
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function deleteSong(id: number): Promise<boolean> {
    isLoading.value = true;
    error.value = null;
    try {
      await SongsService.deleteSong(id);
      // 从歌曲列表中移除歌曲
      songs.value = songs.value.filter((song) => song.id !== id);
      Notify.create({ type: 'positive', message: t('songDeletedSuccessfully'), position: 'top-right' });
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      Notify.create({ type: 'negative', message: t('deleteSongFailed', { error: errorMessage }), position: 'top-right' });
      console.error('Error deleting song:', err);
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function deleteSongsBatch(ids: number[]): Promise<boolean> {
    isLoading.value = true;
    error.value = null;
    try {
      await SongsService.deleteSongsBatch(ids);
      // 从歌曲列表中移除歌曲
      songs.value = songs.value.filter((song) => !ids.includes(song.id));
      Notify.create({ type: 'positive', message: t('songsDeletedSuccessfully'), position: 'top-right' });
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : t('unknownError');
      error.value = errorMessage;
      Notify.create({ type: 'negative', message: t('deleteSongsFailed', { error: errorMessage }), position: 'top-right' });
      console.error('Error deleting songs:', err);
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    // State
    songs,
    isLoading,
    error,
    importTask,

    // Getters
    getSongs,
    getIsLoading,
    getError,
    getImportTask,

    // Actions
    fetchAllSongs,
    importSongs,
    checkImportTaskStatus,
    updateSong,
    deleteSong,
    deleteSongsBatch,
  };
});
