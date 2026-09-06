/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type BackupPreferences = {
    settings?: Record<string, any>;
    motionPreference?: BackupPreferences.motionPreference;
    locale?: BackupPreferences.locale;
    keptCleanupWords?: Array<string>;
};
export namespace BackupPreferences {
    export enum motionPreference {
        ON = 'on',
        OFF = 'off',
    }
    export enum locale {
        EN_US = 'en-US',
        ZH_CN = 'zh-CN',
    }
}
