/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DictionaryEntry } from '../models/DictionaryEntry';
import type { DictionarySource } from '../models/DictionarySource';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DictionaryService {
    /**
     * 获取词典来源与授权信息 / Get dictionary source and license metadata
     * @returns DictionarySource 词典来源与授权信息 / Dictionary source and license metadata
     * @throws ApiError
     */
    public static getDictionarySource(): CancelablePromise<Array<DictionarySource>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/dictionary/source',
        });
    }
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
