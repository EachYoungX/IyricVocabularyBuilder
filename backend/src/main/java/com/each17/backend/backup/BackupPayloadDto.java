package com.each17.backend.backup;

import java.util.List;

public record BackupPayloadDto(
        Integer schemaVersion,
        String appVersion,
        String exportedAt,
        List<SongBackup> songs,
        List<UserVocabularyBackup> userVocabulary,
        List<UserPhraseBackup> userPhrases,
        List<VocabularyOverrideBackup> vocabularyOverrides
) {
    public record SongBackup(
            Long id,
            String title,
            String artist,
            String album,
            String rawTitle,
            String rawArtist,
            String rawSourceContent,
            String lyrics,
            String rawLyrics,
            String normalizedLyrics,
            String lyricsHash,
            Integer importVersion,
            String updatedAt,
            List<LyricLineBackup> lyricLines,
            List<LyricTokenBackup> lyricTokens,
            List<SongCreditBackup> credits
    ) {
    }

    public record LyricLineBackup(
            Long id,
            Integer lineIndex,
            String originalText,
            String normalizedText,
            String lineType,
            String classificationSource,
            Boolean hidden,
            Double confidence,
            Boolean userOverride
    ) {
    }

    public record LyricTokenBackup(
            Long id,
            Long lyricLineId,
            Integer tokenPosition,
            String surfaceForm,
            String normalizedForm,
            String lemma,
            String lemmaStatus,
            Integer startOffset,
            Integer endOffset,
            String tokenType,
            Double learningScore
    ) {
    }

    public record SongCreditBackup(
            Long id,
            String creditType,
            String creditLabel,
            String creditValue,
            Long sourceLineId,
            Integer sortOrder
    ) {
    }

    public record UserVocabularyBackup(
            Long id,
            String userId,
            String lemma,
            String status,
            Double masteryScore,
            String firstSeenAt,
            String lastSeenAt,
            String reviewDueAt,
            String note
    ) {
    }

    public record UserPhraseBackup(
            Long id,
            String userId,
            String canonicalPhrase,
            String definition,
            String createdAt
    ) {
    }

    public record VocabularyOverrideBackup(
            String lemma,
            Boolean excluded,
            Boolean recommendedOverride,
            String updatedAt
    ) {
    }
}
