package com.each17.backend.vocabulary.controller;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.dto.UserPhraseRequestDto;
import com.each17.backend.vocabulary.entity.UserPhrase;
import com.each17.backend.vocabulary.service.UserPhraseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vocabulary/user-phrases")
@RequiredArgsConstructor
public class UserPhraseController {
    private final UserPhraseService userPhraseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserPhrase>>> list() {
        return ResponseEntity.ok(ApiResponse.success(userPhraseService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserPhrase>> add(@Valid @RequestBody UserPhraseRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userPhraseService.add(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userPhraseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
