import type { UserPhrase } from '../models/UserPhrase';
import type { UserPhraseRequest } from '../models/UserPhraseRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class UserPhrasesService {
    public static listUserPhrases(): CancelablePromise<Array<UserPhrase>> {
        return __request(OpenAPI, { method: 'GET', url: '/api/vocabulary/user-phrases' });
    }

    public static addUserPhrase(requestBody: UserPhraseRequest): CancelablePromise<UserPhrase> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/vocabulary/user-phrases',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    public static deleteUserPhrase(id: number): CancelablePromise<void> {
        return __request(OpenAPI, { method: 'DELETE', url: '/api/vocabulary/user-phrases/{id}', path: { id } });
    }
}
