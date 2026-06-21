/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
import type { ImportTaskResult } from '../models/ImportTaskResult';
import type { Song } from '../models/Song';
import type { SongImportRequest } from '../models/SongImportRequest';
import type { SongImportTaskResponse } from '../models/SongImportTaskResponse';
import type { SongUpdateRequest } from '../models/SongUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class SongsService {
    /**
     * 获取所有歌曲 / Get all songs
     * @returns Song 歌曲列表
     * @throws ApiError
     */
    public static getAllSongs(): CancelablePromise<Array<Song>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/songs',
            errors: {
                500: `服务器内部错误 / Internal server error`,
            },
        });
    }
    /**
     * 异步批量导入歌曲（推荐方式）
     * @param requestBody
     * @returns SongImportTaskResponse 导入任务已接受，正在后台处理
     * @throws ApiError
     */
    public static importSongsAsync(
        requestBody: Array<SongImportRequest>,
    ): CancelablePromise<SongImportTaskResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/songs/import',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 查询导入任务完整结果（包含失败详情）
     * @param taskId
     * @returns ImportTaskResult 任务结束（无论成功还是部分失败）后，调用此接口可直接拿到**每首歌的成功/失败原因**。
     * 任务状态为 COMPLETED 或 FAILED 时返回完整详情。
     *
     * @throws ApiError
     */
    public static getImportTaskResult(
        taskId: string,
    ): CancelablePromise<ImportTaskResult> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/songs/import/tasks/{taskId}',
            path: {
                'taskId': taskId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 更新歌曲 / Update song
     * @param id
     * @param requestBody
     * @returns Song 更新成功
     * @throws ApiError
     */
    public static updateSong(
        id: number,
        requestBody: SongUpdateRequest,
    ): CancelablePromise<Song> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/songs/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 删除歌曲 / Delete song
     * @param id
     * @returns void
     * @throws ApiError
     */
    public static deleteSong(
        id: number,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/songs/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 批量删除歌曲 / Delete songs in batch
     * @param requestBody
     * @returns void
     * @throws ApiError
     */
    public static deleteSongsBatch(
        requestBody: Array<number>,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/songs/batch',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                404: `资源未找到 / Resource not found`,
            },
        });
    }
}
