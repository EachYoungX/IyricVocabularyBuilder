package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.*;
import com.each17.backend.vocabulary.entity.VocabularyStatus;

import java.util.List;

public interface UserVocabularyService {
    UserVocabularyDto addWord(UserVocabularyRequestDto request);
    List<UserVocabularyDto> listWords(VocabularyStatus status);
    UserVocabularyDto updateWord(Long id, UserVocabularyUpdateRequestDto request);
    UserVocabularyStatsDto getStats();
    List<UserVocabularyReviewItemDto> getReviewQueue(int limit);
    void deleteWord(Long id);
    void clearAllWords();
}
