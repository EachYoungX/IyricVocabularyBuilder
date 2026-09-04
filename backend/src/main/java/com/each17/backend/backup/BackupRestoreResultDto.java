package com.each17.backend.backup;

import java.util.UUID;

public record BackupRestoreResultDto(
        int restoredSongs,
        int restoredUserVocabulary,
        int restoredUserPhrases,
        UUID vocabularyRebuildTaskId
) {
}
