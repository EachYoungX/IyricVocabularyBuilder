package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyRequestDto {
    private String lemma;
    private String note;
}
