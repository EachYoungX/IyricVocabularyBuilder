package com.each17.backend.backup;

import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {
    private final BackupService backupService;
    private final VocabularyService vocabularyService;

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<BackupPayloadDto>> exportBackup() {
        return ResponseEntity.ok(ApiResponse.success(backupService.exportBackup()));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<BackupValidationResultDto>> validate(
            @RequestBody BackupPayloadDto backup
    ) {
        return ResponseEntity.ok(ApiResponse.success(backupService.validate(backup)));
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<BackupRestoreResultDto>> restore(
            @RequestBody BackupPayloadDto backup,
            @RequestParam(defaultValue = "overwrite") String mode
    ) {
        if (!"overwrite".equalsIgnoreCase(mode)) {
            throw new ValidationException("Only transactional overwrite restore is supported");
        }
        BackupValidationResultDto restored = backupService.restoreOverwrite(backup);
        UUID rebuildTaskId = vocabularyService.refreshVocabularyIndexAsync();
        BackupRestoreResultDto result = new BackupRestoreResultDto(
                restored.songCount(), restored.userVocabularyCount(), restored.userPhraseCount(), rebuildTaskId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
