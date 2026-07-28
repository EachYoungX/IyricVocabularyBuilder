/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { WordOccurrence } from './WordOccurrence';
export type VocabularyQualityCandidate = {
    word: string;
    learningScore: number;
    occurrenceCount: number;
    songCount: number;
    recommended: boolean;
    reasons: Array<'LOW_LEARNING_VALUE' | 'PHRASE_CANDIDATE' | 'CONTRACTION_PHRASE' | 'POSSIBLE_TRUNCATED_LEMMA' | 'VERY_SHORT_TOKEN' | 'NON_STANDARD_TOKEN'>;
    examples: Array<WordOccurrence>;
};

