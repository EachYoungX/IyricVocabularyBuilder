/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { SongCredit } from './SongCredit';
export type Song = {
    id: number;
    title: string;
    artist: string;
    album?: string | null;
    rawTitle?: string | null;
    rawArtist?: string | null;
    rawSourceContent?: string | null;
    lyrics: string;
    credits?: Array<SongCredit>;
};

