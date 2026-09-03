/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type SongImportRequest = {
    title: string;
    artist: string;
    album?: string | null;
    /**
     * 完整原始导入内容（可包含 LRC 元数据和时间戳）
     */
    rawSourceContent?: string | null;
    lyrics: string;
};

