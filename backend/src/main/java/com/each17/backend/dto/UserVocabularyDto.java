package com.each17.backend.dto;

import com.each17.backend.vocabulary.entity.VocabularyStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyDto {
    private Long id;
    private String userId;
    private String lemma;
    private VocabularyStatus status;
    private Double masteryScore;
    private String firstSeenAt;
    private String lastSeenAt;
    private String reviewDueAt;
    private String note;
}
