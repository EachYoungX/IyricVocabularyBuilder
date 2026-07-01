export const APP_SETTINGS_STORAGE_KEY = 'lv-app-settings';

export type FontScale = 'compact' | 'standard' | 'large';
export type InterfaceScale = 'compact' | 'standard' | 'large';
export type ThemeMode = 'midnight-sail' | 'sage-library' | 'coral-study' | 'dusk-minimal' | 'midnight-sail-dark';
export type DefaultNewWordStatus = 'NEW' | 'LEARNING' | 'BOOKMARK_ONLY';
export type LowValueWordHandling = 'QUERY_ONLY' | 'NORMAL' | 'HIDE_RECOMMENDATION_MARKER';
export type PostImportBehavior = 'SAVE_DIRECTLY' | 'CONFIRM_CLEANING';
export type RoleLabelHandling = 'AUTO_HIDE' | 'AUTO_DELETE' | 'KEEP_VISIBLE' | 'CONFIRM_EACH_IMPORT';
export type RepeatedChorusHandling = 'KEEP_ALL' | 'WEAKEN_OR_FOLD' | 'DEDUP_LEARNING_STATS';
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
  repeatedChorusHandling: 'WEAKEN_OR_FOLD',
  fillerWordHandling: 'NOT_RECOMMENDED',
  definitionLanguage: 'BILINGUAL',
  dictionaryDisplay: ['BRIEF', 'PHONETIC_POS', 'LYRIC_CONTEXT'],
  lemmaSearch: true,
  phraseDetection: true,
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function loadAppSettings(): AppSettings {
  const stored = window.localStorage.getItem(APP_SETTINGS_STORAGE_KEY);
  if (!stored) return { ...DEFAULT_APP_SETTINGS };

  try {
    const parsed: unknown = JSON.parse(stored);
    if (!isRecord(parsed)) return { ...DEFAULT_APP_SETTINGS };

    return {
      ...DEFAULT_APP_SETTINGS,
      ...parsed,
      dictionaryDisplay: Array.isArray(parsed.dictionaryDisplay)
        ? (parsed.dictionaryDisplay as DictionaryDisplayItem[])
        : DEFAULT_APP_SETTINGS.dictionaryDisplay,
    };
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
