/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LyricDocument } from '../models/LyricDocument';
import type { LyricImportRequest } from '../models/LyricImportRequest';
import type { LyricLine } from '../models/LyricLine';
import type { LyricLineUpdateRequest } from '../models/LyricLineUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class LyricsService {
    /**
     * 获取结构化歌词 / Get structured lyrics
     * @param songId
     * @returns LyricDocument 原始、标准化与逐行结构化歌词 / Raw, normalized, and structured lyrics
     * @throws ApiError
     */
    public static getStructuredLyrics(
        songId: number,
    ): CancelablePromise<LyricDocument> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/songs/{songId}/lyrics',
            path: {
                'songId': songId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 导入或覆盖歌词 / Import or overwrite lyrics
     * @param songId
     * @param requestBody
     * @returns LyricDocument 导入后的结构化歌词 / Structured lyrics after import
     * @throws ApiError
     */
    public static importLyrics(
        songId: number,
        requestBody: LyricImportRequest,
    ): CancelablePromise<LyricDocument> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/songs/{songId}/lyrics/import',
            path: {
                'songId': songId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                404: `资源未找到 / Resource not found`,
                409: `歌词内容不同且未允许覆盖 / Different lyrics without overwrite permission`,
            },
        });
    }
    /**
     * 修正歌词行 / Correct a lyric line
     * @param songId
     * @param lineId
     * @param requestBody
     * @returns LyricLine 已保存的用户修正 / Saved user correction
     * @throws ApiError
     */
    public static updateLyricLine(
        songId: number,
        lineId: number,
        requestBody: LyricLineUpdateRequest,
    ): CancelablePromise<LyricLine> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/songs/{songId}/lyrics/lines/{lineId}',
            path: {
                'songId': songId,
                'lineId': lineId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
}
