package com.each17.backend.backup;

import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.data.LocalDataService;
import com.each17.backend.lyric.entity.LyricClassificationSource;
import com.each17.backend.lyric.entity.LyricLemmaStatus;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricTokenType;
import com.each17.backend.song.entity.SongCreditType;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BackupService {
    public static final int SCHEMA_VERSION = 1;

    private final JdbcTemplate jdbcTemplate;
    private final String appVersion;
    private final LocalDataService localDataService;

    public BackupService(
            @Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Value("${app.version}") String appVersion,
            LocalDataService localDataService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.appVersion = appVersion;
        this.localDataService = localDataService;
    }

    @Transactional(readOnly = true)
    public BackupPayloadDto exportBackup() {
        Map<Long, List<BackupPayloadDto.LyricLineBackup>> lines = ownedLines();
        Map<Long, List<BackupPayloadDto.LyricTokenBackup>> tokens = ownedTokens();
        Map<Long, List<BackupPayloadDto.SongCreditBackup>> credits = ownedCredits();
        List<BackupPayloadDto.SongBackup> songs = jdbcTemplate.query("""
                SELECT id, title, artist, album, raw_title, raw_artist, raw_source_content,
                       lyrics, raw_lyrics, normalized_lyrics, lyrics_hash, import_version, updated_at
                FROM songs ORDER BY id
                """, (rs, rowNum) -> {
            long songId = rs.getLong("id");
            return new BackupPayloadDto.SongBackup(
                    songId, rs.getString("title"), rs.getString("artist"), rs.getString("album"),
                    rs.getString("raw_title"), rs.getString("raw_artist"), rs.getString("raw_source_content"),
                    rs.getString("lyrics"), rs.getString("raw_lyrics"), rs.getString("normalized_lyrics"),
                    rs.getString("lyrics_hash"), rs.getInt("import_version"), rs.getString("updated_at"),
                    lines.getOrDefault(songId, List.of()), tokens.getOrDefault(songId, List.of()),
                    credits.getOrDefault(songId, List.of()));
        });

        List<BackupPayloadDto.UserVocabularyBackup> vocabulary = jdbcTemplate.query("""
                SELECT id, user_id, lemma, status, mastery_score, first_seen_at, last_seen_at, review_due_at, note
                FROM user_vocabulary ORDER BY id
                """, (rs, rowNum) -> new BackupPayloadDto.UserVocabularyBackup(
                rs.getLong("id"), rs.getString("user_id"), rs.getString("lemma"), rs.getString("status"),
                rs.getDouble("mastery_score"), rs.getString("first_seen_at"), rs.getString("last_seen_at"),
                rs.getString("review_due_at"), rs.getString("note")));

        List<BackupPayloadDto.UserPhraseBackup> phrases = jdbcTemplate.query("""
                SELECT id, user_id, canonical_phrase, definition, created_at
                FROM user_phrase ORDER BY id
                """, (rs, rowNum) -> new BackupPayloadDto.UserPhraseBackup(
                rs.getLong("id"), rs.getString("user_id"), rs.getString("canonical_phrase"),
                rs.getString("definition"), rs.getString("created_at")));

        List<BackupPayloadDto.VocabularyOverrideBackup> overrides = jdbcTemplate.query("""
                SELECT lemma, excluded, recommended_override, updated_at
                FROM vocabulary_override ORDER BY lemma
                """, (rs, rowNum) -> {
            boolean recommended = rs.getBoolean("recommended_override");
            Boolean recommendedOverride = rs.wasNull() ? null : recommended;
            return new BackupPayloadDto.VocabularyOverrideBackup(
                    rs.getString("lemma"), rs.getBoolean("excluded"), recommendedOverride,
                    rs.getString("updated_at"));
        });

        return new BackupPayloadDto(SCHEMA_VERSION, appVersion, OffsetDateTime.now().toString(),
                songs, vocabulary, phrases, overrides, null, null, null);
    }

    public BackupValidationResultDto validate(BackupPayloadDto backup) {
        validatePayload(backup);
        return new BackupValidationResultDto(true, SCHEMA_VERSION, backup.songs().size(),
                backup.userVocabulary().size(), backup.userPhrases().size());
    }

    @Transactional
    public BackupValidationResultDto restoreOverwrite(BackupPayloadDto backup) {
        validatePayload(backup);
        localDataService.clearAll();
        restoreSongs(backup.songs());
        restoreUserVocabulary(backup.userVocabulary());
        restoreUserPhrases(backup.userPhrases());
        restoreOverrides(backup.vocabularyOverrides());
        return new BackupValidationResultDto(true, SCHEMA_VERSION, backup.songs().size(),
                backup.userVocabulary().size(), backup.userPhrases().size());
    }

    private Map<Long, List<BackupPayloadDto.LyricLineBackup>> ownedLines() {
        return jdbcTemplate.query("""
                SELECT id, song_id, line_index, original_text, normalized_text, line_type,
                       classification_source, hidden, confidence, user_override
                FROM lyric_lines ORDER BY song_id, line_index
                """, (rs, rowNum) -> new Owned<>(rs.getLong("song_id"),
                new BackupPayloadDto.LyricLineBackup(
                        rs.getLong("id"), rs.getInt("line_index"), rs.getString("original_text"),
                        rs.getString("normalized_text"), rs.getString("line_type"),
                        rs.getString("classification_source"), rs.getBoolean("hidden"),
                        rs.getDouble("confidence"), rs.getBoolean("user_override"))))
                .stream().collect(Collectors.groupingBy(Owned::ownerId, LinkedHashMap::new,
                        Collectors.mapping(Owned::value, Collectors.toList())));
    }

    private Map<Long, List<BackupPayloadDto.LyricTokenBackup>> ownedTokens() {
        return jdbcTemplate.query("""
                SELECT token.id, line.song_id, token.lyric_line_id, token.token_position,
                       token.surface_form, token.normalized_form, token.lemma, token.lemma_status,
                       token.start_offset, token.end_offset, token.token_type, token.learning_score
                FROM lyric_tokens token
                JOIN lyric_lines line ON line.id = token.lyric_line_id
                ORDER BY line.song_id, token.lyric_line_id, token.token_position
                """, (rs, rowNum) -> new Owned<>(rs.getLong("song_id"),
                new BackupPayloadDto.LyricTokenBackup(
                        rs.getLong("id"), rs.getLong("lyric_line_id"), rs.getInt("token_position"),
                        rs.getString("surface_form"), rs.getString("normalized_form"), rs.getString("lemma"),
                        rs.getString("lemma_status"), rs.getInt("start_offset"), rs.getInt("end_offset"),
                        rs.getString("token_type"), rs.getDouble("learning_score"))))
                .stream().collect(Collectors.groupingBy(Owned::ownerId, LinkedHashMap::new,
                        Collectors.mapping(Owned::value, Collectors.toList())));
    }

    private Map<Long, List<BackupPayloadDto.SongCreditBackup>> ownedCredits() {
        return jdbcTemplate.query("""
                SELECT id, song_id, credit_type, credit_label, credit_value, source_line_id, sort_order
                FROM song_credit ORDER BY song_id, sort_order, id
                """, (rs, rowNum) -> {
            long sourceLine = rs.getLong("source_line_id");
            return new Owned<>(rs.getLong("song_id"), new BackupPayloadDto.SongCreditBackup(
                    rs.getLong("id"), rs.getString("credit_type"), rs.getString("credit_label"),
                    rs.getString("credit_value"), rs.wasNull() ? null : sourceLine, rs.getInt("sort_order")));
        }).stream().collect(Collectors.groupingBy(Owned::ownerId, LinkedHashMap::new,
                Collectors.mapping(Owned::value, Collectors.toList())));
    }

    private void validatePayload(BackupPayloadDto backup) {
        if (backup == null) throw invalid("Backup payload is required");
        if (backup.schemaVersion() == null) throw invalid("schemaVersion is required");
        if (backup.schemaVersion() != SCHEMA_VERSION) {
            throw invalid("Unsupported schemaVersion: " + backup.schemaVersion());
        }
        requireText(backup.appVersion(), "appVersion");
        requireText(backup.exportedAt(), "exportedAt");
        requireList(backup.songs(), "songs");
        requireList(backup.userVocabulary(), "userVocabulary");
        requireList(backup.userPhrases(), "userPhrases");
        requireList(backup.vocabularyOverrides(), "vocabularyOverrides");

        Set<Long> songIds = new HashSet<>();
        Set<String> songKeys = new HashSet<>();
        Set<Long> lineIds = new HashSet<>();
        Set<Long> tokenIds = new HashSet<>();
        Set<Long> creditIds = new HashSet<>();
        for (BackupPayloadDto.SongBackup song : backup.songs()) {
            if (song == null || song.id() == null || song.id() < 1 || !songIds.add(song.id())) {
                throw invalid("Every song must have a unique positive id");
            }
            requireText(song.title(), "song.title");
            requireText(song.artist(), "song.artist");
            requireText(song.lyrics(), "song.lyrics");
            if (!songKeys.add(song.title() + "\u0000" + song.artist())) {
                throw invalid("Duplicate song title/artist: " + song.title());
            }
            requireList(song.lyricLines(), "song.lyricLines");
            requireList(song.lyricTokens(), "song.lyricTokens");
            requireList(song.credits(), "song.credits");
            validateSongChildren(song, lineIds, tokenIds, creditIds);
        }

        validateUserVocabulary(backup.userVocabulary());
        validateUserPhrases(backup.userPhrases());
        validateOverrides(backup.vocabularyOverrides());
    }

    private void validateSongChildren(
            BackupPayloadDto.SongBackup song,
            Set<Long> lineIds,
            Set<Long> tokenIds,
            Set<Long> creditIds
    ) {
        Set<Long> ownLineIds = new HashSet<>();
        Set<Integer> lineIndexes = new HashSet<>();
        for (BackupPayloadDto.LyricLineBackup line : song.lyricLines()) {
            if (line == null || line.id() == null || line.id() < 1 || !lineIds.add(line.id())) {
                throw invalid("Every lyric line must have a unique positive id");
            }
            ownLineIds.add(line.id());
            if (line.lineIndex() == null || line.lineIndex() < 0 || !lineIndexes.add(line.lineIndex())) {
                throw invalid("Lyric line indexes must be unique and non-negative per song");
            }
            requireText(line.originalText(), "lyricLine.originalText");
            requireText(line.normalizedText(), "lyricLine.normalizedText");
            requireEnum(LyricLineType.class, line.lineType(), "lyricLine.lineType");
            requireEnum(LyricClassificationSource.class, line.classificationSource(), "lyricLine.classificationSource");
            if (line.hidden() == null || line.userOverride() == null || line.confidence() == null) {
                throw invalid("Lyric line flags and confidence are required");
            }
        }
        Set<String> positions = new HashSet<>();
        for (BackupPayloadDto.LyricTokenBackup token : song.lyricTokens()) {
            if (token == null || token.id() == null || token.id() < 1 || !tokenIds.add(token.id())) {
                throw invalid("Every lyric token must have a unique positive id");
            }
            if (!ownLineIds.contains(token.lyricLineId())) {
                throw invalid("Lyric token references a line outside its song");
            }
            if (token.tokenPosition() == null || token.tokenPosition() < 0
                    || !positions.add(token.lyricLineId() + ":" + token.tokenPosition())) {
                throw invalid("Lyric token positions must be unique and non-negative per line");
            }
            requireText(token.surfaceForm(), "lyricToken.surfaceForm");
            requireText(token.normalizedForm(), "lyricToken.normalizedForm");
            requireText(token.lemma(), "lyricToken.lemma");
            requireEnum(LyricLemmaStatus.class, token.lemmaStatus(), "lyricToken.lemmaStatus");
            requireEnum(LyricTokenType.class, token.tokenType(), "lyricToken.tokenType");
            if (token.startOffset() == null || token.endOffset() == null || token.learningScore() == null
                    || token.startOffset() < 0 || token.endOffset() < token.startOffset()) {
                throw invalid("Lyric token offsets and learningScore are invalid");
            }
        }
        for (BackupPayloadDto.SongCreditBackup credit : song.credits()) {
            if (credit == null || credit.id() == null || credit.id() < 1 || !creditIds.add(credit.id())) {
                throw invalid("Every song credit must have a unique positive id");
            }
            requireEnum(SongCreditType.class, credit.creditType(), "credit.creditType");
            requireText(credit.creditValue(), "credit.creditValue");
            if (credit.sourceLineId() != null && !ownLineIds.contains(credit.sourceLineId())) {
                throw invalid("Song credit references a line outside its song");
            }
            if (credit.sortOrder() == null || credit.sortOrder() < 0) throw invalid("credit.sortOrder is invalid");
        }
    }

    private void validateUserVocabulary(List<BackupPayloadDto.UserVocabularyBackup> items) {
        Set<Long> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (BackupPayloadDto.UserVocabularyBackup item : items) {
            if (item == null || item.id() == null || item.id() < 1 || !ids.add(item.id())) {
                throw invalid("Every user vocabulary item must have a unique positive id");
            }
            requireText(item.userId(), "userVocabulary.userId");
            requireText(item.lemma(), "userVocabulary.lemma");
            requireEnum(VocabularyStatus.class, item.status(), "userVocabulary.status");
            if (item.masteryScore() == null || item.masteryScore() < 0 || item.masteryScore() > 1) {
                throw invalid("userVocabulary.masteryScore must be between 0 and 1");
            }
            requireText(item.firstSeenAt(), "userVocabulary.firstSeenAt");
            requireText(item.lastSeenAt(), "userVocabulary.lastSeenAt");
            if (!keys.add(item.userId() + "\u0000" + item.lemma())) {
                throw invalid("Duplicate user vocabulary lemma: " + item.lemma());
            }
        }
    }

    private void validateUserPhrases(List<BackupPayloadDto.UserPhraseBackup> items) {
        Set<Long> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (BackupPayloadDto.UserPhraseBackup item : items) {
            if (item == null || item.id() == null || item.id() < 1 || !ids.add(item.id())) {
                throw invalid("Every user phrase must have a unique positive id");
            }
            requireText(item.userId(), "userPhrase.userId");
            requireText(item.canonicalPhrase(), "userPhrase.canonicalPhrase");
            requireText(item.createdAt(), "userPhrase.createdAt");
            if (!keys.add(item.userId() + "\u0000" + item.canonicalPhrase())) {
                throw invalid("Duplicate user phrase: " + item.canonicalPhrase());
            }
        }
    }

    private void validateOverrides(List<BackupPayloadDto.VocabularyOverrideBackup> items) {
        Set<String> lemmas = new HashSet<>();
        for (BackupPayloadDto.VocabularyOverrideBackup item : items) {
            if (item == null) throw invalid("vocabularyOverride cannot be null");
            requireText(item.lemma(), "vocabularyOverride.lemma");
            if (!lemmas.add(item.lemma())) throw invalid("Duplicate vocabulary override: " + item.lemma());
            if (item.excluded() == null) throw invalid("vocabularyOverride.excluded is required");
            requireText(item.updatedAt(), "vocabularyOverride.updatedAt");
        }
    }

    private void restoreSongs(List<BackupPayloadDto.SongBackup> songs) {
        for (BackupPayloadDto.SongBackup song : songs) {
            jdbcTemplate.update("""
                    INSERT INTO songs(id, title, artist, album, raw_title, raw_artist, raw_source_content,
                                      lyrics, raw_lyrics, normalized_lyrics, lyrics_hash, import_version, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, song.id(), song.title(), song.artist(), song.album(), song.rawTitle(), song.rawArtist(),
                    song.rawSourceContent(), song.lyrics(), song.rawLyrics(), song.normalizedLyrics(), song.lyricsHash(),
                    song.importVersion() == null ? 1 : song.importVersion(), song.updatedAt());
            for (BackupPayloadDto.LyricLineBackup line : song.lyricLines()) {
                jdbcTemplate.update("""
                        INSERT INTO lyric_lines(id, song_id, line_index, original_text, normalized_text, line_type,
                                                classification_source, hidden, confidence, user_override)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, line.id(), song.id(), line.lineIndex(), line.originalText(), line.normalizedText(),
                        line.lineType(), line.classificationSource(), line.hidden(), line.confidence(), line.userOverride());
            }
            for (BackupPayloadDto.LyricTokenBackup token : song.lyricTokens()) {
                jdbcTemplate.update("""
                        INSERT INTO lyric_tokens(id, lyric_line_id, token_position, surface_form, normalized_form,
                                                 lemma, lemma_status, start_offset, end_offset, token_type, learning_score)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, token.id(), token.lyricLineId(), token.tokenPosition(), token.surfaceForm(),
                        token.normalizedForm(), token.lemma(), token.lemmaStatus(), token.startOffset(),
                        token.endOffset(), token.tokenType(), token.learningScore());
            }
            for (BackupPayloadDto.SongCreditBackup credit : song.credits()) {
                jdbcTemplate.update("""
                        INSERT INTO song_credit(id, song_id, credit_type, credit_label, credit_value, source_line_id, sort_order)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, credit.id(), song.id(), credit.creditType(), credit.creditLabel(), credit.creditValue(),
                        credit.sourceLineId(), credit.sortOrder());
            }
        }
    }

    private void restoreUserVocabulary(List<BackupPayloadDto.UserVocabularyBackup> items) {
        for (BackupPayloadDto.UserVocabularyBackup item : items) {
            jdbcTemplate.update("""
                    INSERT INTO user_vocabulary(id, user_id, lemma, status, mastery_score, first_seen_at,
                                                last_seen_at, review_due_at, note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, item.id(), item.userId(), item.lemma(), item.status(), item.masteryScore(),
                    item.firstSeenAt(), item.lastSeenAt(), item.reviewDueAt(), item.note());
        }
    }

    private void restoreUserPhrases(List<BackupPayloadDto.UserPhraseBackup> items) {
        for (BackupPayloadDto.UserPhraseBackup item : items) {
            jdbcTemplate.update("""
                    INSERT INTO user_phrase(id, user_id, canonical_phrase, definition, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, item.id(), item.userId(), item.canonicalPhrase(), item.definition(), item.createdAt());
        }
    }

    private void restoreOverrides(List<BackupPayloadDto.VocabularyOverrideBackup> items) {
        for (BackupPayloadDto.VocabularyOverrideBackup item : items) {
            jdbcTemplate.update("""
                    INSERT INTO vocabulary_override(lemma, excluded, recommended_override, updated_at)
                    VALUES (?, ?, ?, ?)
                    """, item.lemma(), item.excluded(), item.recommendedOverride(), item.updatedAt());
        }
    }

    private <T> void requireList(List<T> value, String field) {
        if (value == null) throw invalid(field + " is required");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw invalid(field + " is required");
    }

    private <E extends Enum<E>> void requireEnum(Class<E> type, String value, String field) {
        requireText(value, field);
        try {
            Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid(field + " is invalid: " + value);
        }
    }

    private ValidationException invalid(String message) {
        return new ValidationException("Invalid backup: " + message);
    }

    private record Owned<T>(Long ownerId, T value) {
    }
}
