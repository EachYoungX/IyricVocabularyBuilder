package com.each17.backend.dto;

import com.each17.backend.lyric.entity.LyricLemmaStatus;
import com.each17.backend.lyric.entity.LyricTokenType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordOccurrenceDto {
    private Long songId;
    private String songTitle;
    private String songArtist;
    private Long lyricLineId;
    private Integer lineIndex;
    private String lyricLine;
    private Integer tokenPosition;
    private String surfaceForm;
    private String normalizedForm;
    private String lemma;
    private LyricLemmaStatus lemmaStatus;
    private LyricTokenType tokenType;
    private Integer startOffset;
    private Integer endOffset;
    private Double learningScore;
}
