import { describe, expect, it } from 'vitest';
import { DEFAULT_APP_SETTINGS } from './appSettings';
import {
  APP_LOCALE_STORAGE_KEY,
  clearClientPreferences,
  createClientPreferences,
  extractClientPreferences,
  KEPT_CLEANUP_WORDS_STORAGE_KEY,
  normalizeKeptCleanupWords,
  SEARCH_HISTORY_STORAGE_KEY,
} from './clientPreferences';
import { APP_SETTINGS_STORAGE_KEY } from './appSettings';
import { MOTION_STORAGE_KEY } from './motionPreference';

describe('client backup preferences', () => {
  it('builds the complete preferences wrapper used by full backup export', () => {
    expect(createClientPreferences(
      DEFAULT_APP_SETTINGS,
      'off',
      'zh-CN',
      ['zeta', 'alpha', 'alpha'],
    )).toEqual({
      settings: DEFAULT_APP_SETTINGS,
      motionPreference: 'off',
      locale: 'zh-CN',
      keptCleanupWords: ['alpha', 'zeta'],
    });
  });

  it('normalizes nested preferences and falls back invalid setting fields', () => {
    const normalized = extractClientPreferences({
      preferences: {
        settings: { themeMode: 'not-a-theme', phraseDetection: false },
        motionPreference: 'on',
        locale: 'en-US',
        keptCleanupWords: [' keep ', '', 42, 'keep', 'also-keep'],
      },
    });

    expect(normalized).toMatchObject({
      settings: { themeMode: DEFAULT_APP_SETTINGS.themeMode, phraseDetection: false },
      motionPreference: 'on',
      locale: 'en-US',
      keptCleanupWords: ['also-keep', 'keep'],
    });
  });

  it('accepts legacy top-level settings and motion fields', () => {
    const normalized = extractClientPreferences({
      settings: { themeMode: 'coral-study' },
      motionPreference: 'off',
    });

    expect(normalized?.settings?.themeMode).toBe('coral-study');
    expect(normalized?.motionPreference).toBe('off');
  });

  it('rejects invalid kept-word payloads and bounds normalized entries', () => {
    expect(normalizeKeptCleanupWords('word')).toBeUndefined();
    expect(normalizeKeptCleanupWords([' valid ', 'x'.repeat(201)])).toEqual(['valid']);
  });

  it('clears every persisted client preference used by the application', () => {
    const removed: string[] = [];
    clearClientPreferences({ removeItem: (key) => { removed.push(key); } });

    expect(removed).toEqual([
      APP_SETTINGS_STORAGE_KEY,
      MOTION_STORAGE_KEY,
      APP_LOCALE_STORAGE_KEY,
      KEPT_CLEANUP_WORDS_STORAGE_KEY,
      SEARCH_HISTORY_STORAGE_KEY,
    ]);
  });
});
