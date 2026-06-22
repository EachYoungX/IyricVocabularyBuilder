import { Loading, Notify, useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { VocabularyService } from 'src/services/api';
import { VocabularyRebuildTask } from 'src/services/api/models/VocabularyRebuildTask';
import { useVocabularyStore } from 'src/stores/vocabularyStore';

const POLL_INTERVAL_MS = 2_000;
const MAX_POLL_ATTEMPTS = 90;

export function useVocabularyRebuild() {
  const $q = useQuasar();
  const { t } = useI18n();
  const vocabularyStore = useVocabularyStore();

  function requestVocabularyRebuild() {
    $q.dialog({
      title: t('tearDownIndexTitle'),
      message: t('tearDownIndexMessage'),
      html: true,
      ok: { label: t('tearDownNow'), color: 'primary', unelevated: true },
      cancel: { label: t('cancel'), flat: true, color: 'grey' },
      persistent: true,
    }).onOk(() => void startVocabularyRebuild());
  }

  async function startVocabularyRebuild() {
    if (Loading.isActive) {
      Notify.create({ message: t('rebuildInProgress'), type: 'info' });
      return;
    }

    Loading.show({ message: t('startingRebuild'), spinnerColor: 'primary' });
    try {
      const response = await VocabularyService.refreshVocabularyIndex();
      if (!response?.taskId) throw new Error(t('noTaskIdReturned'));
      await pollRefreshStatus(response.taskId);
    } catch (error) {
      const message = error instanceof Error ? error.message : t('unknownError');
      Notify.create({ type: 'negative', message: `${t('rebuildFailed')}: ${message}`, icon: 'error' });
    } finally {
      Loading.hide();
    }
  }

  function pollRefreshStatus(taskId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      let attempts = 0;

      const poll = async () => {
        attempts += 1;
        try {
          const task = await VocabularyService.getRefreshTaskStatus(taskId);
          if (task.status === VocabularyRebuildTask.status.COMPLETED) {
            await vocabularyStore.fetchWords({ page: 0, size: 50 });
            Notify.create({ message: t('rebuildSuccess'), type: 'positive', icon: 'check_circle', position: 'top' });
            resolve();
            return;
          }
          if (task.status === VocabularyRebuildTask.status.FAILED) {
            reject(new Error(t('rebuildTaskFailedOnServer')));
            return;
          }
          if (attempts >= MAX_POLL_ATTEMPTS) {
            reject(new Error(t('refreshTaskTimedOut')));
            return;
          }
          setTimeout(() => void poll(), POLL_INTERVAL_MS);
        } catch (error) {
          reject(error instanceof Error ? error : new Error(t('failedToCheckRefreshStatus')));
        }
      };

      void poll();
    });
  }

  return { requestVocabularyRebuild };
}
