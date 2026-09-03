/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DictionaryEntry } from './DictionaryEntry';
import type { PhraseMatch } from './PhraseMatch';
export type LyricTokenContext = {
    lyricLineId?: number;
    tokenPosition?: number;
    surfaceForm?: string;
    normalizedForm?: string;
    lemma?: string;
    lemmaStatus?: string;
    tokenType?: string;
    startOffset?: number;
    endOffset?: number;
    wordEntry?: DictionaryEntry;
    phraseMatches?: Array<PhraseMatch>;
};

