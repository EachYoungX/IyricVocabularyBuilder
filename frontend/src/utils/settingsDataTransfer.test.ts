import { describe, expect, it } from 'vitest';
import { VocabularyStatus } from 'src/services/api';
import { parseVocabularyText } from './settingsDataTransfer';

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
});
