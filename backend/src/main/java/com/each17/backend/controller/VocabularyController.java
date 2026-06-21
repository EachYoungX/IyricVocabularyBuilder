package com.each17.backend.controller;

import com.each17.backend.dto.*;
import com.each17.backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping("/words")
    public ResponseEntity<WordPageDto> getWordList(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(vocabularyService.getWordList(prefix, page, size));
    }

    @GetMapping("/words/{word}/occurrences")
    public ResponseEntity<List<WordOccurrenceDto>> getWordOccurrences(@PathVariable String word) {
        return ResponseEntity.ok(vocabularyService.getWordOccurrences(word));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SongImportResponseDto> refreshVocabularyIndex() { // <-- [修正] 返回包含 taskId 的标准 DTO
        UUID taskId = vocabularyService.refreshVocabularyIndexAsync();

        SongImportResponseDto response = SongImportResponseDto.builder()
                .taskId(taskId)
                .message("Vocabulary index rebuild task started")
                .build();

        return ResponseEntity.accepted().body(response);
    }

    // [新增] 进度查询接口
    @GetMapping("/refresh/tasks/{taskId}")
    public ResponseEntity<VocabularyRebuildTaskDto> getRefreshTaskStatus(@PathVariable UUID taskId) {
        return ResponseEntity.ok(vocabularyService.getRefreshTaskStatus(taskId));
    }
}