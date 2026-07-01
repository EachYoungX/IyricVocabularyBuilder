package com.each17.backend.dto;

import com.each17.backend.vocabulary.entity.VocabularyStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyReviewItemDto {
    private Long id;
    private String lemma;
    private VocabularyStatus status;
    private Double masteryScore;
    private String reviewDueAt;
    private WordOccurrenceDto example;
}
