/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { UserVocabulary } from '../models/UserVocabulary';
import type { UserVocabularyRequest } from '../models/UserVocabularyRequest';
import type { UserVocabularyReviewItem } from '../models/UserVocabularyReviewItem';
import type { UserVocabularyStats } from '../models/UserVocabularyStats';
import type { UserVocabularyUpdateRequest } from '../models/UserVocabularyUpdateRequest';
import type { VocabularyStatus } from '../models/VocabularyStatus';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UserVocabularyService {
    /**
     * 加入个人词库 / Add a word to personal vocabulary
     * @param requestBody
     * @returns UserVocabulary 已保存的个人词条 / Saved personal vocabulary item
     * @throws ApiError
     */
    public static addUserVocabularyWord(
        requestBody: UserVocabularyRequest,
    ): CancelablePromise<UserVocabulary> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/user-vocabulary',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 查询个人词库 / List personal vocabulary
     * @param status
     * @returns UserVocabulary 个人词条列表 / Personal vocabulary items
     * @throws ApiError
     */
    public static listUserVocabularyWords(
        status?: VocabularyStatus,
    ): CancelablePromise<Array<UserVocabulary>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/user-vocabulary',
            query: {
                'status': status,
            },
        });
    }
    /**
     * 清空个人词库 / Clear personal vocabulary
     * @returns void
     * @throws ApiError
     */
    public static clearUserVocabularyWords(): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/user-vocabulary',
        });
    }
    /**
     * 更新个人词条 / Update a personal vocabulary item
     * @param id
     * @param requestBody
     * @returns UserVocabulary 更新后的个人词条 / Updated personal vocabulary item
     * @throws ApiError
     */
    public static updateUserVocabularyWord(
        id: number,
        requestBody: UserVocabularyUpdateRequest,
    ): CancelablePromise<UserVocabulary> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/user-vocabulary/{id}',
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
     * 获取个人词汇统计 / Get personal vocabulary stats
     * @returns UserVocabularyStats 个人词汇统计 / Personal vocabulary stats
     * @throws ApiError
     */
    public static getUserVocabularyStats(): CancelablePromise<UserVocabularyStats> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/user-vocabulary/stats',
        });
    }
    /**
     * 获取待复习词条 / Get review queue
     * @param limit
     * @returns UserVocabularyReviewItem 待复习词条 / Review queue
     * @throws ApiError
     */
    public static getUserVocabularyReviewQueue(
        limit: number = 10,
    ): CancelablePromise<Array<UserVocabularyReviewItem>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/user-vocabulary/review',
            query: {
                'limit': limit,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
}
