package com.each17.backend.lyric.controller;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.lyric.dto.*;
import com.each17.backend.lyric.service.LyricStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/songs/{songId}/lyrics")
@RequiredArgsConstructor
public class LyricController {
    private final LyricStructureService lyricStructureService;

    @GetMapping
    public ResponseEntity<ApiResponse<LyricDocumentDto>> getLyrics(@PathVariable Long songId) {
        return ResponseEntity.ok(ApiResponse.success(lyricStructureService.getDocument(songId)));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<LyricDocumentDto>> importLyrics(
            @PathVariable Long songId,
            @RequestBody LyricImportRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(lyricStructureService.importLyrics(songId, request)));
    }

    @PutMapping("/lines/{lineId}")
    public ResponseEntity<ApiResponse<LyricLineDto>> updateLine(
            @PathVariable Long songId,
            @PathVariable Long lineId,
            @RequestBody LyricLineUpdateRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(lyricStructureService.updateLine(songId, lineId, request)));
    }
}
