/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BackupPayload } from '../models/BackupPayload';
import type { BackupRestoreResult } from '../models/BackupRestoreResult';
import type { BackupValidationResult } from '../models/BackupValidationResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class BackupService {
    /**
     * 导出完整备份 / Export a versioned full backup
     * @returns BackupPayload 完整备份
     * @throws ApiError
     */
    public static exportBackup(): CancelablePromise<BackupPayload> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/backup/export',
        });
    }
    /**
     * 在修改数据前校验备份 / Validate a backup before any mutation
     * @param requestBody
     * @returns BackupValidationResult 校验结果
     * @throws ApiError
     */
    public static validateBackup(
        requestBody: BackupPayload,
    ): CancelablePromise<BackupValidationResult> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/backup/validate',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 事务性覆盖恢复 / Transactional overwrite restore
     * @param requestBody
     * @param mode
     * @returns BackupRestoreResult 恢复结果
     * @throws ApiError
     */
    public static restoreBackup(
        requestBody: BackupPayload,
        mode: 'overwrite' = 'overwrite',
    ): CancelablePromise<BackupRestoreResult> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/backup/restore',
            query: {
                'mode': mode,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
}
