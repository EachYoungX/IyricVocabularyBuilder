/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DictionaryEntry } from '../models/DictionaryEntry';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DictionaryService {
    /**
     * 查询英英词典 / Lookup English word
     * @param word 英文单词（不区分大小写）
     * @returns DictionaryEntry 词典条目
     * @throws ApiError
     */
    public static lookupDictionaryWord(
        word: string,
    ): CancelablePromise<DictionaryEntry> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/dictionary/{word}',
            path: {
                'word': word,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
                404: `资源未找到 / Resource not found`,
            },
        });
    }
}
