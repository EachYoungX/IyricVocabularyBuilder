/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LyricLine } from './LyricLine';
import type { SongCredit } from './SongCredit';
export type LyricDocument = {
    songId: number;
    title: string;
    artist: string;
    album?: string | null;
    rawLyrics: string;
    normalizedLyrics: string;
    lyricsHash: string;
    importVersion: number;
    updatedAt: string;
    lines: Array<LyricLine>;
    credits: Array<SongCredit>;
};

