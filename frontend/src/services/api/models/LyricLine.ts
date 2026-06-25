/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LyricLineType } from './LyricLineType';
export type LyricLine = {
    id: number;
    lineIndex: number;
    originalText: string;
    normalizedText: string;
    lineType: LyricLineType;
    hidden: boolean;
    confidence: number;
    userOverride: boolean;
};

