export type DictionaryPhrase = {
    id: number;
    sourcePattern: string;
    canonicalPattern: string;
    definitionEn?: string;
    definitionZh?: string;
    usageNoteZh?: string;
    phraseType: string;
    source: string;
    tokenCountMin: number;
    tokenCountMax: number;
    matchPriority: number;
};
