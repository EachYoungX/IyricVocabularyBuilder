/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { VocabularyStatus } from './VocabularyStatus';
import type { WordOccurrence } from './WordOccurrence';
export type UserVocabularyReviewItem = {
    id: number;
    lemma: string;
    status: VocabularyStatus;
    masteryScore: number;
    reviewDueAt?: string | null;
    example?: WordOccurrence | null;
};

