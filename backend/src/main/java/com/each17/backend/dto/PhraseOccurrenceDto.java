package com.each17.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhraseOccurrenceDto {
    private Long phraseId;
    private Long songId;
    private String songTitle;
    private String songArtist;
    private Long lyricLineId;
    private Integer lineIndex;
    private String lyricLine;
    private Integer startTokenPosition;
    private Integer endTokenPosition;
    private String surfacePhrase;
}
