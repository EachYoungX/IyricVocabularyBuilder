package com.each17.backend.dictionary.model;

public record PhraseEntry(
        Long id,
        String sourcePattern,
        String canonicalPattern,
        String definitionEn,
        String definitionZh,
        String usageNoteZh,
        String phraseType,
        String source,
        int tokenCountMin,
        int tokenCountMax,
        int matchPriority
) {
}
