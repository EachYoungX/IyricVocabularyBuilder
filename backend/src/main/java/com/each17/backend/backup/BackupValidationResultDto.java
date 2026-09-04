package com.each17.backend.backup;

public record BackupValidationResultDto(
        boolean valid,
        int schemaVersion,
        int songCount,
        int userVocabularyCount,
        int userPhraseCount
) {
}
