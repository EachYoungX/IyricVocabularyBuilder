import {
  APP_SETTINGS_STORAGE_KEY,
  normalizeAppSettings,
  type AppSettings,
} from 'src/utils/appSettings';
import {
  MOTION_STORAGE_KEY,
  type MotionPreference,
} from 'src/utils/motionPreference';
import { WELCOME_DISMISSED_KEY } from 'src/utils/firstLaunch';

export const APP_LOCALE_STORAGE_KEY = 'app-locale';
export const KEPT_CLEANUP_WORDS_STORAGE_KEY = 'lv-kept-cleanup-candidate-words';
export const SEARCH_HISTORY_STORAGE_KEY = 'lv-search-history';

export type SupportedLocale = 'en-US' | 'zh-CN';

export type ClientPreferences = {
  settings: AppSettings;
  motionPreference: MotionPreference;
  locale: SupportedLocale;
  keptCleanupWords: string[];
};

export type NormalizedClientPreferences = Partial<ClientPreferences>;

type StorageReader = Pick<Storage, 'getItem'>;
type StorageWriter = Pick<Storage, 'setItem'>;
type StorageRemover = Pick<Storage, 'removeItem'>;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function normalizeMotionPreference(value: unknown): MotionPreference | undefined {
  return value === 'on' || value === 'off' ? value : undefined;
}

function normalizeLocale(value: unknown): SupportedLocale | undefined {
  return value === 'en-US' || value === 'zh-CN' ? value : undefined;
}

export function normalizeKeptCleanupWords(value: unknown): string[] | undefined {
  if (!Array.isArray(value)) return undefined;
  return [...new Set(value
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter((item) => item.length > 0 && item.length <= 200)
    .slice(0, 10_000))]
    .sort();
}

export function createClientPreferences(
  settings: AppSettings,
  motionPreference: MotionPreference,
  locale: string,
  keptCleanupWords: Iterable<string>,
): ClientPreferences {
  return {
    settings,
    motionPreference,
    locale: normalizeLocale(locale) ?? 'en-US',
    keptCleanupWords: normalizeKeptCleanupWords([...keptCleanupWords]) ?? [],
  };
}

export function extractClientPreferences(value: unknown): NormalizedClientPreferences | null {
  if (!isRecord(value)) return null;
  const nested = isRecord(value.preferences) ? value.preferences : {};
  const settings = normalizeAppSettings(nested.settings ?? value.settings);
  const motionPreference = normalizeMotionPreference(nested.motionPreference ?? value.motionPreference);
  const locale = normalizeLocale(nested.locale);
  const keptCleanupWords = normalizeKeptCleanupWords(nested.keptCleanupWords);

  if (!settings && !motionPreference && !locale && !keptCleanupWords) return null;
  return {
    ...(settings ? { settings } : {}),
    ...(motionPreference ? { motionPreference } : {}),
    ...(locale ? { locale } : {}),
    ...(keptCleanupWords ? { keptCleanupWords } : {}),
  };
}

export function loadKeptCleanupWords(storage: StorageReader = window.localStorage): Set<string> {
  try {
    const normalized = normalizeKeptCleanupWords(JSON.parse(
      storage.getItem(KEPT_CLEANUP_WORDS_STORAGE_KEY) ?? '[]',
    ));
    return new Set(normalized ?? []);
  } catch {
    return new Set();
  }
}

export function saveKeptCleanupWords(
  words: Iterable<string>,
  storage: StorageWriter = window.localStorage,
) {
  const normalized = normalizeKeptCleanupWords([...words]) ?? [];
  storage.setItem(KEPT_CLEANUP_WORDS_STORAGE_KEY, JSON.stringify(normalized));
}

export function clearClientPreferences(storage: StorageRemover = window.localStorage) {
  for (const key of [
    APP_SETTINGS_STORAGE_KEY,
    MOTION_STORAGE_KEY,
    APP_LOCALE_STORAGE_KEY,
    KEPT_CLEANUP_WORDS_STORAGE_KEY,
    SEARCH_HISTORY_STORAGE_KEY,
    WELCOME_DISMISSED_KEY,
  ]) {
    storage.removeItem(key);
  }
}
