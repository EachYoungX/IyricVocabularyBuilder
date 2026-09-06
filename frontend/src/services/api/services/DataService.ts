/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LocalDataResetResult } from '../models/LocalDataResetResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DataService {
    /**
     * 事务性清除全部本地用户数据 / Transactionally clear all local user data
     * @returns LocalDataResetResult 本地数据已清除
     * @throws ApiError
     */
    public static clearAllLocalData(): CancelablePromise<LocalDataResetResult> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/data/all',
        });
    }
}
