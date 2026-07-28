export const APP_SETTINGS_STORAGE_KEY = 'lv-app-settings';

export type FontScale = 'compact' | 'standard' | 'large';
export type InterfaceScale = 'compact' | 'standard' | 'large';
export type ThemeMode = 'midnight-sail' | 'sage-library' | 'coral-study' | 'dusk-minimal' | 'midnight-sail-dark';
export type DefaultNewWordStatus = 'NEW' | 'LEARNING';
export type LowValueWordHandling = 'QUERY_ONLY' | 'NORMAL' | 'HIDE_RECOMMENDATION_MARKER';
export type PostImportBehavior = 'SAVE_DIRECTLY' | 'CONFIRM_CLEANING';
export type RoleLabelHandling = 'AUTO_HIDE' | 'AUTO_DELETE' | 'KEEP_VISIBLE' | 'CONFIRM_EACH_IMPORT';
export type RepeatedChorusHandling = 'KEEP_ALL' | 'DEDUP_LEARNING_STATS';
export type FillerWordHandling = 'NOT_RECOMMENDED' | 'NORMAL' | 'EXCLUDE_STATS';
export type DefinitionLanguage = 'ZH' | 'EN' | 'BILINGUAL';
export type DictionaryDisplayItem = 'BRIEF' | 'FULL' | 'PHONETIC_POS' | 'INFLECTIONS' | 'LYRIC_CONTEXT';

export type AppSettings = {
  fontScale: FontScale;
  interfaceScale: InterfaceScale;
  themeMode: ThemeMode;
  defaultNewWordStatus: DefaultNewWordStatus;
  lowValueWordHandling: LowValueWordHandling;
  postImportBehavior: PostImportBehavior;
  roleLabelHandling: RoleLabelHandling;
  repeatedChorusHandling: RepeatedChorusHandling;
  fillerWordHandling: FillerWordHandling;
  definitionLanguage: DefinitionLanguage;
  dictionaryDisplay: DictionaryDisplayItem[];
  lemmaSearch: boolean;
  phraseDetection: boolean;
};

export const DEFAULT_APP_SETTINGS: AppSettings = {
  fontScale: 'standard',
  interfaceScale: 'standard',
  themeMode: 'midnight-sail',
  defaultNewWordStatus: 'NEW',
  lowValueWordHandling: 'QUERY_ONLY',
  postImportBehavior: 'CONFIRM_CLEANING',
  roleLabelHandling: 'AUTO_HIDE',
  repeatedChorusHandling: 'KEEP_ALL',
  fillerWordHandling: 'NOT_RECOMMENDED',
  definitionLanguage: 'BILINGUAL',
  dictionaryDisplay: ['BRIEF', 'PHONETIC_POS', 'LYRIC_CONTEXT'],
  lemmaSearch: true,
  phraseDetection: true,
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function pickValue<T extends string>(value: unknown, allowed: readonly T[], fallback: T): T {
  return allowed.includes(value as T) ? (value as T) : fallback;
}

function pickBoolean(value: unknown, fallback: boolean) {
  return typeof value === 'boolean' ? value : fallback;
}

export function normalizeAppSettings(value: unknown): AppSettings | null {
  if (!isRecord(value)) return null;

  return {
    fontScale: pickValue(value.fontScale, ['compact', 'standard', 'large'], DEFAULT_APP_SETTINGS.fontScale),
    interfaceScale: pickValue(
      value.interfaceScale,
      ['compact', 'standard', 'large'],
      DEFAULT_APP_SETTINGS.interfaceScale,
    ),
    themeMode: pickValue(
      value.themeMode,
      ['midnight-sail', 'sage-library', 'coral-study', 'dusk-minimal', 'midnight-sail-dark'],
      DEFAULT_APP_SETTINGS.themeMode,
    ),
    defaultNewWordStatus: pickValue(
      value.defaultNewWordStatus,
      ['NEW', 'LEARNING'],
      DEFAULT_APP_SETTINGS.defaultNewWordStatus,
    ),
    lowValueWordHandling: pickValue(
      value.lowValueWordHandling,
      ['QUERY_ONLY', 'NORMAL', 'HIDE_RECOMMENDATION_MARKER'],
      DEFAULT_APP_SETTINGS.lowValueWordHandling,
    ),
    postImportBehavior: pickValue(
      value.postImportBehavior,
      ['SAVE_DIRECTLY', 'CONFIRM_CLEANING'],
      DEFAULT_APP_SETTINGS.postImportBehavior,
    ),
    roleLabelHandling: pickValue(
      value.roleLabelHandling,
      ['AUTO_HIDE', 'AUTO_DELETE', 'KEEP_VISIBLE', 'CONFIRM_EACH_IMPORT'],
      DEFAULT_APP_SETTINGS.roleLabelHandling,
    ),
    repeatedChorusHandling: normalizeRepeatedChorusHandling(value.repeatedChorusHandling),
    fillerWordHandling: pickValue(
      value.fillerWordHandling,
      ['NOT_RECOMMENDED', 'NORMAL', 'EXCLUDE_STATS'],
      DEFAULT_APP_SETTINGS.fillerWordHandling,
    ),
    definitionLanguage: pickValue(
      value.definitionLanguage,
      ['ZH', 'EN', 'BILINGUAL'],
      DEFAULT_APP_SETTINGS.definitionLanguage,
    ),
    dictionaryDisplay: Array.isArray(value.dictionaryDisplay)
      ? value.dictionaryDisplay.filter((item): item is DictionaryDisplayItem =>
          ['BRIEF', 'FULL', 'PHONETIC_POS', 'INFLECTIONS', 'LYRIC_CONTEXT'].includes(String(item)),
        )
      : DEFAULT_APP_SETTINGS.dictionaryDisplay,
    lemmaSearch: pickBoolean(value.lemmaSearch, DEFAULT_APP_SETTINGS.lemmaSearch),
    phraseDetection: pickBoolean(value.phraseDetection, DEFAULT_APP_SETTINGS.phraseDetection),
  };
}

function normalizeRepeatedChorusHandling(value: unknown): RepeatedChorusHandling {
  if (value === 'DEDUP_LEARNING_STATS') return 'DEDUP_LEARNING_STATS';
  return 'KEEP_ALL';
}

export function loadAppSettings(): AppSettings {
  const stored = window.localStorage.getItem(APP_SETTINGS_STORAGE_KEY);
  if (!stored) return { ...DEFAULT_APP_SETTINGS };

  try {
    const parsed: unknown = JSON.parse(stored);
    return normalizeAppSettings(parsed) ?? { ...DEFAULT_APP_SETTINGS };
  } catch {
    return { ...DEFAULT_APP_SETTINGS };
  }
}

export function applyAppSettings(settings: AppSettings) {
  document.documentElement.dataset.theme = settings.themeMode;
  document.documentElement.dataset.fontScale = settings.fontScale;
  document.documentElement.dataset.interfaceScale = settings.interfaceScale;
}

export function saveAppSettings(settings: AppSettings) {
  window.localStorage.setItem(APP_SETTINGS_STORAGE_KEY, JSON.stringify(settings));
  applyAppSettings(settings);
}
