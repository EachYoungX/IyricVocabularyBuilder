package com.each17.backend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhraseMatchDto {
    Long phraseId;
    String sourcePattern;
    String canonicalPattern;
    String definitionEn;
    String definitionZh;
    String usageNoteZh;
    String phraseType;
    String source;
    int startTokenPosition;
    int endTokenPosition;
    String surfacePhrase;
    int matchPriority;
}
