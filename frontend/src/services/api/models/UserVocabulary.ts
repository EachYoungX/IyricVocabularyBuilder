/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { VocabularyStatus } from './VocabularyStatus';
export type UserVocabulary = {
    id: number;
    userId: string;
    lemma: string;
    status: VocabularyStatus;
    masteryScore: number;
    firstSeenAt: string;
    lastSeenAt: string;
    reviewDueAt?: string | null;
    note?: string | null;
};

