/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TaskCreationResponse } from '../models/TaskCreationResponse';
import type { VocabularyBulkWordsRequest } from '../models/VocabularyBulkWordsRequest';
import type { VocabularyLearningValueRequest } from '../models/VocabularyLearningValueRequest';
import type { VocabularyQualityCandidate } from '../models/VocabularyQualityCandidate';
import type { VocabularyRebuildTask } from '../models/VocabularyRebuildTask';
import type { WordOccurrence } from '../models/WordOccurrence';
import type { WordPage } from '../models/WordPage';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class VocabularyService {
    /**
     * 获取唯一单词分页列表 / Get paginated unique words
     * 返回所有英文歌词中提取的唯一单词（已排序、不区分大小写去重），支持前缀过滤和分页。
     * @param prefix 前缀过滤（不区分大小写） / Case-insensitive prefix filter
     * @param page 页码（从0开始） / Page number (0-indexed)
     * @param size 每页条数 / Page size
     * @param recommendedOnly 是否只返回推荐学习词 / Whether to return recommended words only
     * @param lemmaSearch 是否使用词形归一搜索 / Whether to use lemmatized search
     * @returns WordPage 分页单词列表
     * @throws ApiError
     */
    public static getWordList(
        prefix?: string,
        page?: number,
        size: number = 50,
        recommendedOnly: boolean = true,
        lemmaSearch: boolean = true,
    ): CancelablePromise<WordPage> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/words',
            query: {
                'prefix': prefix,
                'page': page,
                'size': size,
                'recommendedOnly': recommendedOnly,
                'lemmaSearch': lemmaSearch,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                500: `服务器内部错误 / Internal server error`,
            },
        });
    }
    /**
     * 批量删除词汇索引项 / Delete vocabulary index words
     * 删除词汇索引中的指定词条，不删除歌曲原文。
     * @param requestBody
     * @returns number 实际删除数量
     * @throws ApiError
     */
    public static deleteVocabularyWords(
        requestBody: VocabularyBulkWordsRequest,
    ): CancelablePromise<number> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/vocabulary/words',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 获取单词出现位置 / Get word occurrences in lyrics
     * @param word 要查询的英文单词（不区分大小写）
     * @returns WordOccurrence 出现位置列表
     * @throws ApiError
     */
    public static getWordOccurrences(
        word: string,
    ): CancelablePromise<Array<WordOccurrence>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/words/{word}/occurrences',
            path: {
                'word': word,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                404: `词汇表中未找到该单词`,
                500: `服务器内部错误 / Internal server error`,
            },
        });
    }
    /**
     * 手动更新词条学习价值 / Manually update word learning value
     * 将词条标记为推荐学习或低学习价值，不删除歌曲原文。
     * @param word
     * @param requestBody
     * @returns VocabularyQualityCandidate 更新后的词条摘要
     * @throws ApiError
     */
    public static updateVocabularyLearningValue(
        word: string,
        requestBody: VocabularyLearningValueRequest,
    ): CancelablePromise<VocabularyQualityCandidate> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/vocabulary/words/{word}/learning-value',
            path: {
                'word': word,
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
     * 获取词库清洗候选 / Get vocabulary cleanup candidates
     * 返回疑似低学习价值或异常词条，用于人工确认后清洗。
     * @param limit
     * @returns VocabularyQualityCandidate 词库清洗候选
     * @throws ApiError
     */
    public static getVocabularyQualityCandidates(
        limit: number = 80,
    ): CancelablePromise<Array<VocabularyQualityCandidate>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/quality-candidates',
            query: {
                'limit': limit,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 重新构建词汇索引 / Rebuild vocabulary index
     * 触发异步任务重新从所有歌曲提取词汇索引
     * @param requestBody
     * @returns TaskCreationResponse 异步重建任务已接受
     * @throws ApiError
     */
    public static refreshVocabularyIndex(
        requestBody?: Record<string, any>,
    ): CancelablePromise<TaskCreationResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/vocabulary/refresh',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `服务器内部错误 / Internal server error`,
            },
        });
    }
    /**
     * 查询词汇索引刷新任务状态
     * 根据任务 ID 查询词汇索引重建任务的当前进度和最终结果。
     * @param taskId 从 /refresh 接口获取的任务 ID
     * @returns VocabularyRebuildTask 任务的当前状态或最终结果
     * @throws ApiError
     */
    public static getRefreshTaskStatus(
        taskId: string,
    ): CancelablePromise<VocabularyRebuildTask> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/refresh/tasks/{taskId}',
            path: {
                'taskId': taskId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
}
