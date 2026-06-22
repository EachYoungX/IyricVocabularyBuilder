package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;

import java.util.List;
import java.util.UUID;

public interface VocabularyService {
    WordPageDto getWordList(String prefix, int page, int size);
    List<WordOccurrenceDto> getWordOccurrences(String word);
    UUID refreshVocabularyIndexAsync();
    VocabularyRebuildTaskDto getRefreshTaskStatus(UUID taskId);
}
