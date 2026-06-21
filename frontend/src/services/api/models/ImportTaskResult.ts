/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ImportTaskResult = {
    taskId: string;
    status: ImportTaskResult.status;
    total: number;
    successCount: number;
    failedCount: number;
    startedAt?: string;
    finishedAt?: string | null;
    failedItems?: Array<{
        /**
         * 在本次提交的数组中的下标（前端可据此高亮显示）
         */
        index: number;
        title?: string | null;
        artist?: string | null;
        lyricsSnippet?: string | null;
        error: string;
    }>;
};
export namespace ImportTaskResult {
    export enum status {
        PENDING = 'PENDING',
        RUNNING = 'RUNNING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}

