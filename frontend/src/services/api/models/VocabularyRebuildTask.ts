/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type VocabularyRebuildTask = {
    taskId: string;
    status: VocabularyRebuildTask.status;
    startedAt: string;
    finishedAt?: string | null;
    errorMessage?: string | null;
};
export namespace VocabularyRebuildTask {
    export enum status {
        PENDING = 'PENDING',
        RUNNING = 'RUNNING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}

