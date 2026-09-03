import type { DictionaryPhrase } from '../models/DictionaryPhrase';
import type { LyricTokenContext } from '../models/LyricTokenContext';
import type { PhraseMatch } from '../models/PhraseMatch';
import type { PhraseOccurrence } from '../models/PhraseOccurrence';
import type { PhrasePage } from '../models/PhrasePage';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class PhrasesService {
    public static listPhrases(q?: string, page: number = 0, size: number = 50): CancelablePromise<PhrasePage> {
        return __request(OpenAPI, {
            method: 'GET', url: '/api/vocabulary/phrases', query: { q, page, size },
        });
    }

    public static searchPhrases(q: string, limit: number = 50): CancelablePromise<Array<DictionaryPhrase>> {
        return __request(OpenAPI, { method: 'GET', url: '/api/vocabulary/phrases/search', query: { q, limit } });
    }

    public static getSongPhrases(songId: number): CancelablePromise<Array<PhraseMatch>> {
        return __request(OpenAPI, { method: 'GET', url: '/api/songs/{songId}/phrases', path: { songId } });
    }

    public static getPhraseOccurrences(phraseId: number): CancelablePromise<Array<PhraseOccurrence>> {
        return __request(OpenAPI, {
            method: 'GET', url: '/api/vocabulary/phrases/{phraseId}/occurrences', path: { phraseId },
        });
    }

    public static refreshSongPhrases(songId: number): CancelablePromise<void> {
        return __request(OpenAPI, { method: 'POST', url: '/api/songs/{songId}/phrases/refresh', path: { songId } });
    }

    public static getTokenContext(lineId: number, position: number): CancelablePromise<LyricTokenContext> {
        return __request(OpenAPI, {
            method: 'GET', url: '/api/lyrics/lines/{lineId}/tokens/{position}',
            path: { lineId, position },
        });
    }
}
