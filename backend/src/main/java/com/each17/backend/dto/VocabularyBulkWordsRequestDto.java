package com.each17.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class VocabularyBulkWordsRequestDto {
    private List<String> words;
}
