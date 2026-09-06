/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BackupPreferences } from './BackupPreferences';
/**
 * Versioned source-data backup. Song items include structured lyric lines, tokens, and credits.
 */
export type BackupPayload = {
    schemaVersion: BackupPayload.schemaVersion;
    appVersion: string;
    exportedAt: string;
    songs: Array<Record<string, any>>;
    userVocabulary: Array<Record<string, any>>;
    userPhrases: Array<Record<string, any>>;
    vocabularyOverrides: Array<Record<string, any>>;
    preferences?: BackupPreferences;
    settings?: Record<string, any>;
    motionPreference?: BackupPayload.motionPreference;
};
export namespace BackupPayload {
    export enum schemaVersion {
        '_1' = 1,
    }
    export enum motionPreference {
        ON = 'on',
        OFF = 'off',
    }
}
