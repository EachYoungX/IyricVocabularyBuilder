package com.each17.backend.dto;

import com.each17.backend.vocabulary.entity.VocabularyStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyUpdateRequestDto {
    private VocabularyStatus status;
    private Double masteryScore;
    private String note;
}
