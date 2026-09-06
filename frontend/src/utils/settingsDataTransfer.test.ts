import { describe, expect, it } from 'vitest';
import { VocabularyStatus } from 'src/services/api';
import { parseVocabularyText, vocabularyToCsv } from './settingsDataTransfer';

describe('parseVocabularyText', () => {
  it('parses quoted CSV fields and supported statuses', () => {
    const result = parseVocabularyText(
      'lemma,status,note\njourney,LEARNING,"heard in songs, often"',
      'vocabulary.csv',
    );

    expect(result).toEqual([{
      lemma: 'journey',
      status: VocabularyStatus.LEARNING,
      note: 'heard in songs, often',
    }]);
  });

  it('accepts localized TSV headers and drops invalid empty rows', () => {
    const result = parseVocabularyText(
      '单词\t状态\t备注\nanchor\tMASTERED\tchorus<br>example\n\tNEW\tignored',
      'vocabulary.tsv',
    );

    expect(result).toEqual([{
      lemma: 'anchor',
      status: VocabularyStatus.MASTERED,
      note: 'chorus\nexample',
    }]);
  });

  it('leaves unknown statuses unset for backend defaults', () => {
    const [item] = parseVocabularyText('lemma,status\nharbor,UNKNOWN', 'vocabulary.csv');

    expect(item).toEqual({ lemma: 'harbor', status: undefined, note: null });
  });

  it('accepts every generated vocabulary status used by the real import path', () => {
    const result = parseVocabularyText(
      'lemma,status,note\nknown,FAMILIAR,review later\nmarked,BOOKMARK_ONLY,reference',
      'vocabulary.csv',
    );

    expect(result.map((item) => item.status)).toEqual([
      VocabularyStatus.FAMILIAR,
      VocabularyStatus.BOOKMARK_ONLY,
    ]);
  });

  it('exports the documented migration columns without precise learning timestamps', () => {
    const csv = vocabularyToCsv([{
      id: 1,
      userId: 'local',
      lemma: 'journey',
      status: VocabularyStatus.LEARNING,
      masteryScore: 0.25,
      firstSeenAt: '2026-09-01T00:00:00Z',
      lastSeenAt: '2026-09-02T00:00:00Z',
      reviewDueAt: '2026-09-03T00:00:00Z',
      note: 'chorus',
    }]);

    expect(csv).toBe('"lemma","status","note"\n"journey","LEARNING","chorus"');
    expect(csv).not.toContain('masteryScore');
  });
});
