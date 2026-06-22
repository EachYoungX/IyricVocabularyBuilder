package com.each17.backend.vocabulary.controller;

import com.each17.backend.dto.*;
import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.vocabulary.service.VocabularyService;
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
    public ResponseEntity<ApiResponse<WordPageDto>> getWordList(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(vocabularyService.getWordList(prefix, page, size)));
    }

    @GetMapping("/words/{word}/occurrences")
    public ResponseEntity<ApiResponse<List<WordOccurrenceDto>>> getWordOccurrences(@PathVariable String word) {
        return ResponseEntity.ok(ApiResponse.success(vocabularyService.getWordOccurrences(word)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SongImportResponseDto>> refreshVocabularyIndex() {
        UUID taskId = vocabularyService.refreshVocabularyIndexAsync();

        SongImportResponseDto response = SongImportResponseDto.builder()
                .taskId(taskId)
                .message("Vocabulary index rebuild task started")
                .build();

        return ResponseEntity.accepted().body(ApiResponse.accepted(response));
    }

    // [新增] 进度查询接口
    @GetMapping("/refresh/tasks/{taskId}")
    public ResponseEntity<ApiResponse<VocabularyRebuildTaskDto>> getRefreshTaskStatus(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(vocabularyService.getRefreshTaskStatus(taskId)));
    }
}
