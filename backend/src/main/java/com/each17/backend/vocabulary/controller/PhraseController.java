package com.each17.backend.vocabulary.controller;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.dto.PhraseOccurrenceDto;
import com.each17.backend.dto.PhrasePageDto;
import com.each17.backend.vocabulary.service.PhraseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PhraseController {
    private final PhraseQueryService phraseQueryService;

    @GetMapping("/api/vocabulary/phrases")
    public ResponseEntity<ApiResponse<PhrasePageDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(phraseQueryService.getPhrasePage(q, page, size)));
    }

    @GetMapping("/api/vocabulary/phrases/search")
    public ResponseEntity<ApiResponse<List<PhraseEntry>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(phraseQueryService.searchPhrases(q, limit)));
    }

    @GetMapping("/api/songs/{songId}/phrases")
    public ResponseEntity<ApiResponse<List<PhraseMatchDto>>> songPhrases(@PathVariable Long songId) {
        return ResponseEntity.ok(ApiResponse.success(phraseQueryService.getSongPhrases(songId)));
    }

    @GetMapping("/api/vocabulary/phrases/{phraseId}/occurrences")
    public ResponseEntity<ApiResponse<List<PhraseOccurrenceDto>>> phraseOccurrences(@PathVariable Long phraseId) {
        return ResponseEntity.ok(ApiResponse.success(phraseQueryService.getPhraseOccurrences(phraseId)));
    }

    @PostMapping("/api/songs/{songId}/phrases/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(@PathVariable Long songId) {
        phraseQueryService.refreshSongPhrases(songId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
