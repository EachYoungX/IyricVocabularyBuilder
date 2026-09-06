import { describe, expect, it } from 'vitest';
import {
  DEFAULT_APP_SETTINGS,
  normalizeAppSettings,
} from './appSettings';

describe('normalizeAppSettings', () => {
  it('keeps a complete valid settings object', () => {
    const settings = {
      ...DEFAULT_APP_SETTINGS,
      fontScale: 'large' as const,
      themeMode: 'sage-library' as const,
      defaultNewWordStatus: 'LEARNING' as const,
      autoAddImportedWords: true,
      dictionaryDisplay: ['FULL', 'INFLECTIONS'] as const,
      phraseDetection: false,
    };

    expect(normalizeAppSettings(settings)).toEqual(settings);
  });

  it('falls back field by field when enum and boolean values are invalid', () => {
    const normalized = normalizeAppSettings({
      themeMode: 'unknown-theme',
      roleLabelHandling: 'UNKNOWN',
      autoAddImportedWords: 'yes',
      lemmaSearch: 1,
    });

    expect(normalized).toMatchObject({
      themeMode: DEFAULT_APP_SETTINGS.themeMode,
      roleLabelHandling: DEFAULT_APP_SETTINGS.roleLabelHandling,
      autoAddImportedWords: DEFAULT_APP_SETTINGS.autoAddImportedWords,
      lemmaSearch: DEFAULT_APP_SETTINGS.lemmaSearch,
    });
  });

  it('rejects non-object settings payloads', () => {
    expect(normalizeAppSettings(null)).toBeNull();
    expect(normalizeAppSettings([])).toBeNull();
  });
});
