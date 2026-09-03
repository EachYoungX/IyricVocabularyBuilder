/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type SongCredit = {
    id?: number;
    creditType: SongCredit.creditType;
    creditLabel?: string | null;
    creditValue: string;
    sourceLineId?: number | null;
    sortOrder?: number;
};
export namespace SongCredit {
    export enum creditType {
        LYRICIST = 'LYRICIST',
        COMPOSER = 'COMPOSER',
        PRODUCER = 'PRODUCER',
        CO_PRODUCER = 'CO_PRODUCER',
        EXECUTIVE_PRODUCER = 'EXECUTIVE_PRODUCER',
        MIXING_ENGINEER = 'MIXING_ENGINEER',
        MASTERING_ENGINEER = 'MASTERING_ENGINEER',
        ARRANGER = 'ARRANGER',
        PERFORMER = 'PERFORMER',
        VOCALS = 'VOCALS',
        GUITAR = 'GUITAR',
        BASS = 'BASS',
        DRUMS = 'DRUMS',
        PIANO = 'PIANO',
        KEYBOARD = 'KEYBOARD',
        VIOLIN = 'VIOLIN',
        FEATURING = 'FEATURING',
        OTHER = 'OTHER',
    }
}

