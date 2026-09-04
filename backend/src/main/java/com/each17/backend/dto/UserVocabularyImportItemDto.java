package com.each17.backend.dto;

import com.each17.backend.vocabulary.entity.VocabularyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyImportItemDto {
    private String lemma;
    private VocabularyStatus status;
    private String note;
}
