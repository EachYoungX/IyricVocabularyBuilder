package com.each17.backend.dto;

import com.each17.backend.lyric.entity.LyricLemmaStatus;
import com.each17.backend.lyric.entity.LyricTokenType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LyricTokenContextDto {
    Long lyricLineId;
    Integer tokenPosition;
    String surfaceForm;
    String normalizedForm;
    String lemma;
    LyricLemmaStatus lemmaStatus;
    LyricTokenType tokenType;
    Integer startOffset;
    Integer endOffset;
    DictionaryEntryDto wordEntry;
    List<PhraseMatchDto> phraseMatches;
}
