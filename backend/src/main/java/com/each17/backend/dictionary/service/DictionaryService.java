package com.each17.backend.dictionary.service;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;

public interface DictionaryService {
    DictionaryEntryDto lookupWord(String word);
    DictionarySourceDto getSourceInfo();
}
