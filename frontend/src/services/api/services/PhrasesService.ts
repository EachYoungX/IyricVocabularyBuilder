/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DictionaryPhrase } from '../models/DictionaryPhrase';
import type { LyricTokenContext } from '../models/LyricTokenContext';
import type { PhraseMatch } from '../models/PhraseMatch';
import type { PhraseOccurrence } from '../models/PhraseOccurrence';
import type { PhrasePage } from '../models/PhrasePage';
import type { UserPhrase } from '../models/UserPhrase';
import type { UserPhraseRequest } from '../models/UserPhraseRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class PhrasesService {
    /**
     * 查询歌词 token 及关联短语 / Get token context and phrase matches
     * @param lineId
     * @param position
     * @returns LyricTokenContext token、词典条目与上下文短语
     * @throws ApiError
     */
    public static getLyricTokenContext(
        lineId: number,
        position: number,
    ): CancelablePromise<LyricTokenContext> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/lyrics/lines/{lineId}/tokens/{position}',
            path: {
                'lineId': lineId,
                'position': position,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 分页查询歌曲中实际命中的正式短语 / Get paginated phrases matched in songs
     * @param q 在歌曲实际命中的短语中进行包含匹配 / Contains search among phrases matched in songs
     * @param page
     * @param size
     * @returns PhrasePage 分页短语列表
     * @throws ApiError
     */
    public static listPhrases(
        q?: string,
        page?: number,
        size: number = 50,
    ): CancelablePromise<PhrasePage> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/phrases',
            query: {
                'q': q,
                'page': page,
                'size': size,
            },
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 搜索歌曲中实际命中的正式短语 / Search dictionary phrases matched in songs
     * @param q 在歌曲实际命中的短语中进行包含匹配 / Contains search among phrases matched in songs
     * @param limit
     * @returns DictionaryPhrase 短语列表
     * @throws ApiError
     */
    public static searchPhrases(
        q: string,
        limit: number = 50,
    ): CancelablePromise<Array<DictionaryPhrase>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/phrases/search',
            query: {
                'q': q,
                'limit': limit,
            },
        });
    }
    /**
     * 查询歌曲中的正式短语 / Get phrases in a song
     * @param songId
     * @returns PhraseMatch 歌曲短语命中
     * @throws ApiError
     */
    public static getSongPhrases(
        songId: number,
    ): CancelablePromise<Array<PhraseMatch>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/songs/{songId}/phrases',
            path: {
                'songId': songId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 查询短语出现位置 / Get phrase occurrences
     * @param phraseId
     * @returns PhraseOccurrence 短语出现位置列表
     * @throws ApiError
     */
    public static getPhraseOccurrences(
        phraseId: number,
    ): CancelablePromise<Array<PhraseOccurrence>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/phrases/{phraseId}/occurrences',
            path: {
                'phraseId': phraseId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 重建歌曲短语缓存 / Refresh song phrase cache
     * @param songId
     * @returns any 缓存已刷新
     * @throws ApiError
     */
    public static refreshSongPhrases(
        songId: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/songs/{songId}/phrases/refresh',
            path: {
                'songId': songId,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
    /**
     * 查询用户短语 / List user phrases
     * @returns UserPhrase 用户自定义短语
     * @throws ApiError
     */
    public static listUserPhrases(): CancelablePromise<Array<UserPhrase>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/vocabulary/user-phrases',
        });
    }
    /**
     * 添加用户短语 / Add a user phrase
     * @param requestBody
     * @returns UserPhrase 已保存的用户短语
     * @throws ApiError
     */
    public static addUserPhrase(
        requestBody: UserPhraseRequest,
    ): CancelablePromise<UserPhrase> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/vocabulary/user-phrases',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求参数错误 / Invalid request parameters`,
            },
        });
    }
    /**
     * 删除用户短语 / Delete a user phrase
     * @param id
     * @returns any 已删除
     * @throws ApiError
     */
    public static deleteUserPhrase(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/vocabulary/user-phrases/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `资源未找到 / Resource not found`,
            },
        });
    }
}
