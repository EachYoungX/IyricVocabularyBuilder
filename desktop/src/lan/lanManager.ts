import type { RuntimeConfig } from '../config/runtimeConfig';
import { RuntimeConfigStore } from '../config/runtimeConfig';

type ActiveDictionaryFile = () => Promise<string | null>;
type RestartBackend = (dictionaryFile: string | null, lanEnabled: boolean) => Promise<void>;

export class LanManager {
  constructor(
    private readonly configStore: RuntimeConfigStore,
    private readonly activeDictionaryFile: ActiveDictionaryFile,
    private readonly restartBackend: RestartBackend,
  ) {}

  async setEnabled(enabled: boolean) {
    const previous = await this.configStore.load();
    if (previous.lan.enabled === enabled) return;
    const dictionaryFile = await this.activeDictionaryFile();
    const next: RuntimeConfig = {
      ...previous,
      lan: { enabled },
    };
    await this.configStore.save(next);
    try {
      await this.restartBackend(dictionaryFile, enabled);
    } catch (switchError) {
      await this.configStore.save(previous);
      try {
        await this.restartBackend(dictionaryFile, previous.lan.enabled);
      } catch (rollbackError) {
        throw new AggregateError(
          [switchError, rollbackError],
          'LAN mode switch failed and the previous backend could not be restored',
          { cause: rollbackError },
        );
      }
      throw new Error('LAN mode switch failed; the previous setting was restored', {
        cause: switchError,
      });
    }
  }
}
