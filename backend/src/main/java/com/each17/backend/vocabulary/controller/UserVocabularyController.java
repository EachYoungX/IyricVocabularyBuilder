package com.each17.backend.vocabulary.controller;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.dto.*;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import com.each17.backend.vocabulary.service.UserVocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-vocabulary")
@RequiredArgsConstructor
public class UserVocabularyController {
    private final UserVocabularyService userVocabularyService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserVocabularyDto>> addWord(@RequestBody UserVocabularyRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userVocabularyService.addWord(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserVocabularyDto>>> listWords(
            @RequestParam(required = false) VocabularyStatus status) {
        return ResponseEntity.ok(ApiResponse.success(userVocabularyService.listWords(status)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserVocabularyDto>> updateWord(
            @PathVariable Long id,
            @RequestBody UserVocabularyUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userVocabularyService.updateWord(id, request)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserVocabularyStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(userVocabularyService.getStats()));
    }

    @GetMapping("/review")
    public ResponseEntity<ApiResponse<List<UserVocabularyReviewItemDto>>> getReviewQueue(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(userVocabularyService.getReviewQueue(limit)));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAllWords() {
        userVocabularyService.clearAllWords();
        return ResponseEntity.noContent().build();
    }
}
