export type PhrasePage = {
    content: Array<import('./DictionaryPhrase').DictionaryPhrase>;
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
};
