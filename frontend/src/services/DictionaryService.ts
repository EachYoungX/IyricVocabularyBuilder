import { OpenAPI } from './api/core/OpenAPI';
import { request as __request } from './api/core/request';
import type { CancelablePromise } from './api/core/CancelablePromise';
import type { DictionaryEntry } from './api/models/DictionaryEntry';

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
    });
  }
}
