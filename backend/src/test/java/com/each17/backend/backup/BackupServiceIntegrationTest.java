package com.each17.backend.backup;

import com.each17.backend.common.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BackupServiceIntegrationTest {
    @Autowired
    private BackupService backupService;

    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearData();
        seedSourceData();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_song_restore");
        clearData();
    }

    @Test
    void validatesSchemaBeforeRestore() {
        BackupPayloadDto missingVersion = new BackupPayloadDto(
                null, "1.0.0", "2026-09-04T00:00:00Z", List.of(), List.of(), List.of(), List.of(),
                null, null, null);
        assertThrows(ValidationException.class, () -> backupService.restoreOverwrite(missingVersion));
        assertEquals(1, count("songs"));

        BackupPayloadDto unsupported = new BackupPayloadDto(
                99, "1.0.0", "2026-09-04T00:00:00Z", List.of(), List.of(), List.of(), List.of(),
                null, null, null);
        assertThrows(ValidationException.class, () -> backupService.restoreOverwrite(unsupported));
        assertEquals(1, count("songs"));
    }

    @Test
    void exportsAndRestoresAllUserSourceFields() {
        BackupPayloadDto backup = backupService.exportBackup();
        assertEquals(BackupService.SCHEMA_VERSION, backup.schemaVersion());
        assertEquals("Album", backup.songs().getFirst().album());
        assertEquals("raw file", backup.songs().getFirst().rawSourceContent());
        assertEquals(true, backup.songs().getFirst().lyricLines().getFirst().userOverride());
        assertEquals(1, backup.songs().getFirst().credits().size());
        assertEquals("note", backup.userVocabulary().getFirst().note());
        assertEquals("2026-09-05T00:00:00", backup.userVocabulary().getFirst().reviewDueAt());
        assertEquals("hold on", backup.userPhrases().getFirst().canonicalPhrase());

        jdbcTemplate.update("UPDATE songs SET album = 'changed'");
        jdbcTemplate.update("DELETE FROM user_phrase");
        backupService.restoreOverwrite(backup);

        BackupPayloadDto restored = backupService.exportBackup();
        assertEquals("Album", restored.songs().getFirst().album());
        assertEquals("raw file", restored.songs().getFirst().rawSourceContent());
        assertEquals(true, restored.songs().getFirst().lyricLines().getFirst().hidden());
        assertEquals("MANUAL", restored.songs().getFirst().lyricLines().getFirst().classificationSource());
        assertEquals(1, restored.songs().getFirst().lyricTokens().size());
        assertEquals(1, restored.songs().getFirst().credits().size());
        assertEquals("LEARNING", restored.userVocabulary().getFirst().status());
        assertEquals(0.4, restored.userVocabulary().getFirst().masteryScore());
        assertEquals("hold on", restored.userPhrases().getFirst().canonicalPhrase());
        assertEquals(false, restored.vocabularyOverrides().getFirst().recommendedOverride());
    }

    @Test
    void rollsBackOverwriteWhenAnInsertFails() {
        BackupPayloadDto backup = backupService.exportBackup();
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_song_restore BEFORE INSERT ON songs
                BEGIN SELECT RAISE(ABORT, 'forced restore failure'); END
                """);

        assertThrows(RuntimeException.class, () -> backupService.restoreOverwrite(backup));

        assertEquals(1, count("songs"));
        assertEquals("Original", jdbcTemplate.queryForObject("SELECT title FROM songs", String.class));
        assertEquals(1, count("user_vocabulary"));
        assertEquals(1, count("user_phrase"));
    }

    private void seedSourceData() {
        jdbcTemplate.update("""
                INSERT INTO songs(id, title, artist, album, raw_title, raw_artist, raw_source_content,
                                  lyrics, raw_lyrics, normalized_lyrics, lyrics_hash, import_version, updated_at)
                VALUES (1, 'Original', 'Artist', 'Album', 'Raw title', 'Raw artist', 'raw file',
                        'lyrics', 'raw lyrics', 'normalized', 'hash', 1, '2026-09-04T00:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO lyric_lines(id, song_id, line_index, original_text, normalized_text, line_type,
                                        classification_source, hidden, confidence, user_override)
                VALUES (10, 1, 0, 'line', 'line', 'LYRIC', 'MANUAL', 1, 0.9, 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO lyric_tokens(id, lyric_line_id, token_position, surface_form, normalized_form,
                                         lemma, lemma_status, start_offset, end_offset, token_type, learning_score)
                VALUES (20, 10, 0, 'Line', 'line', 'line', 'VERIFIED', 0, 4, 'WORD', 1.0)
                """);
        jdbcTemplate.update("""
                INSERT INTO song_credit(id, song_id, credit_type, credit_label, credit_value, source_line_id, sort_order)
                VALUES (30, 1, 'LYRICIST', 'Lyrics', 'Writer', 10, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO user_vocabulary(id, user_id, lemma, status, mastery_score, first_seen_at,
                                            last_seen_at, review_due_at, note)
                VALUES (40, 'local', 'line', 'LEARNING', 0.4, '2026-09-01T00:00:00',
                        '2026-09-04T00:00:00', '2026-09-05T00:00:00', 'note')
                """);
        jdbcTemplate.update("""
                INSERT INTO user_phrase(id, user_id, canonical_phrase, definition, created_at)
                VALUES (50, 'local', 'hold on', 'wait', '2026-09-04T00:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO vocabulary_override(lemma, excluded, recommended_override, updated_at)
                VALUES ('line', 0, 0, '2026-09-04T00:00:00')
                """);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void clearData() {
        for (String table : List.of("phrase_occurrence", "phrase_cache_state", "vocabulary_occurrences", "song_credit",
                "lyric_tokens", "lyric_lines", "songs", "user_phrase", "user_vocabulary",
                "vocabulary_override", "vocabulary")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }
}
