package com.each17.backend.data;

import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class LocalDataController {
    private final LocalDataService localDataService;
    private final VocabularyService vocabularyService;

    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<LocalDataResetResultDto>> clearAllLocalData() {
        localDataService.clearAll();
        UUID rebuildTaskId = vocabularyService.refreshVocabularyIndexAsync();
        return ResponseEntity.ok(ApiResponse.success(new LocalDataResetResultDto(rebuildTaskId)));
    }
}
