package com.each17.backend.dictionary.service;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;

import java.util.List;
import java.util.Optional;

public interface DictionaryService {
    DictionaryEntryDto lookupWord(String word);
    default Optional<DictionaryEntryDto> findWord(String word) {
        try {
            return Optional.ofNullable(lookupWord(word));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
    List<DictionarySourceDto> getSourceInfo();
}
