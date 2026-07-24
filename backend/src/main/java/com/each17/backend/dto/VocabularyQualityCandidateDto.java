package com.each17.backend.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyQualityCandidateDto {
    private String word;
    private Double learningScore;
    private Integer occurrenceCount;
    private Integer songCount;
    private Boolean recommended;
    private List<String> reasons;
    private List<WordOccurrenceDto> examples;
}
