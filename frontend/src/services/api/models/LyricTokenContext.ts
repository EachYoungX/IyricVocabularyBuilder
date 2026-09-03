import type { DictionaryEntry } from './DictionaryEntry';
import type { PhraseMatch } from './PhraseMatch';
export type LyricTokenContext = {
    lyricLineId: number;
    tokenPosition: number;
    surfaceForm: string;
    normalizedForm: string;
    lemma: string;
    lemmaStatus: 'VERIFIED' | 'FALLBACK' | 'UNKNOWN';
    tokenType: 'WORD' | 'CONTRACTION' | 'LOW_VALUE';
    startOffset: number;
    endOffset: number;
    wordEntry?: DictionaryEntry;
    phraseMatches: Array<PhraseMatch>;
};
