package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordOccurrenceDto {
    private Long songId;
    private String songTitle;
    private Long lyricLineId;
    private Integer lineIndex;
    private String lyricLine;
    private String surfaceForm;
    private String lemma;
    private Integer startOffset;
    private Integer endOffset;
    private Double learningScore;
}
