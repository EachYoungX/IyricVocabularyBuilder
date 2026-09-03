/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LyricLineType } from './LyricLineType';
import type { LyricClassificationSource } from './LyricClassificationSource';
export type LyricLine = {
    id: number;
    lineIndex: number;
    originalText: string;
    normalizedText: string;
    lineType: LyricLineType;
    classificationSource: LyricClassificationSource;
    hidden: boolean;
    confidence: number;
    userOverride: boolean;
};
