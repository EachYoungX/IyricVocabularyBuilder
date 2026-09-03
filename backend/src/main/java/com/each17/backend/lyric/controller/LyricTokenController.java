package com.each17.backend.lyric.controller;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.dto.LyricTokenContextDto;
import com.each17.backend.vocabulary.service.PhraseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lyrics/lines")
@RequiredArgsConstructor
public class LyricTokenController {
    private final PhraseQueryService phraseQueryService;

    @GetMapping("/{lineId}/tokens/{position}")
    public ResponseEntity<ApiResponse<LyricTokenContextDto>> getTokenContext(
            @PathVariable Long lineId,
            @PathVariable int position) {
        return ResponseEntity.ok(ApiResponse.success(phraseQueryService.getTokenContext(lineId, position)));
    }
}
