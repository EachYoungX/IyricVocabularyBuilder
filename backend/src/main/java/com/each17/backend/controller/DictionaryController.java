package com.each17.backend.controller;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/{word}")
    public ResponseEntity<DictionaryEntryDto> lookupWord(@PathVariable String word) {
        return ResponseEntity.ok(dictionaryService.lookupWord(word));
    }
}