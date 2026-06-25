package com.each17.backend.dictionary.controller;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;
import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.dictionary.service.DictionaryService;
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

    @GetMapping("/source")
    public ResponseEntity<ApiResponse<DictionarySourceDto>> getSourceInfo() {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getSourceInfo()));
    }

    @GetMapping("/{word}")
    public ResponseEntity<ApiResponse<DictionaryEntryDto>> lookupWord(@PathVariable String word) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.lookupWord(word)));
    }
}
