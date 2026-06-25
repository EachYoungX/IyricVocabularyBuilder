/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LyricLine } from './LyricLine';
export type LyricDocument = {
    songId: number;
    rawLyrics: string;
    normalizedLyrics: string;
    lyricsHash: string;
    importVersion: number;
    updatedAt: string;
    lines: Array<LyricLine>;
};

