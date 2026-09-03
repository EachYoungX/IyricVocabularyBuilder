export type PhraseMatch = {
    phraseId: number | null;
    sourcePattern: string;
    canonicalPattern: string;
    definitionEn?: string;
    definitionZh?: string;
    usageNoteZh?: string;
    phraseType: string;
    source: string;
    startTokenPosition: number;
    endTokenPosition: number;
    surfacePhrase: string;
    matchPriority: number;
};
