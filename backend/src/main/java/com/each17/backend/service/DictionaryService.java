package com.each17.backend.service;

import com.each17.backend.dto.DictionaryEntryDto;

public interface DictionaryService {
    DictionaryEntryDto lookupWord(String word);
}