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
    reasons: Array<string>;
    examples: Array<WordOccurrence>;
};

