package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.VocabularyQualityCandidateDto;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;

import java.util.List;
import java.util.UUID;

public interface VocabularyService {
    WordPageDto getWordList(String prefix, int page, int size, boolean recommendedOnly, boolean lemmaSearch, boolean includePhrases);
    List<WordOccurrenceDto> getWordOccurrences(String word);
    List<VocabularyQualityCandidateDto> getQualityCandidates(int limit);
    UUID refreshVocabularyIndexAsync();
    VocabularyRebuildTaskDto getRefreshTaskStatus(UUID taskId);
}
