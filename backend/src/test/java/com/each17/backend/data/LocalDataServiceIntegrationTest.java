package com.each17.backend.data;

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
class LocalDataServiceIntegrationTest {
    private static final List<String> CLEARED_TABLES = List.of(
            "phrase_occurrence", "phrase_cache_state", "vocabulary_occurrences", "song_credit",
            "lyric_tokens", "lyric_lines", "songs", "user_phrase", "user_vocabulary",
            "vocabulary_override", "vocabulary"
    );

    @Autowired
    private LocalDataService localDataService;
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_local_data_reset");
        localDataService.clearAll();
        jdbcTemplate.update("DELETE FROM app_meta WHERE key = 'reset-test'");
        seedEveryUserDataTable();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_local_data_reset");
        localDataService.clearAll();
        jdbcTemplate.update("DELETE FROM app_meta WHERE key = 'reset-test'");
    }

    @Test
    void clearsEveryUserAndDerivedTableButKeepsApplicationMetadata() {
        localDataService.clearAll();

        for (String table : CLEARED_TABLES) {
            assertEquals(0, count(table), table + " should be empty");
        }
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_meta WHERE key = 'reset-test'", Integer.class));
    }

    @Test
    void failureRollsBackTheEntireReset() {
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_local_data_reset BEFORE DELETE ON user_phrase
                BEGIN SELECT RAISE(ABORT, 'forced reset failure'); END
                """);

        assertThrows(RuntimeException.class, () -> localDataService.clearAll());

        for (String table : CLEARED_TABLES) {
            assertEquals(1, count(table), table + " should be restored by rollback");
        }
    }

    private void seedEveryUserDataTable() {
        jdbcTemplate.update("""
                INSERT INTO songs(id, title, artist, lyrics, import_version)
                VALUES (9101, 'Reset Test', 'Integration', 'clear all local data', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO lyric_lines(id, song_id, line_index, original_text, normalized_text,
                                        line_type, classification_source, hidden, confidence, user_override)
                VALUES (9102, 9101, 0, 'clear all local data', 'clear all local data',
                        'LYRIC', 'DEFAULT', 0, 1.0, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO song_credit(id, song_id, credit_type, credit_value, source_line_id, sort_order)
                VALUES (9103, 9101, 'OTHER', 'Integration', 9102, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO lyric_tokens(id, lyric_line_id, token_position, surface_form, normalized_form,
                                         lemma, lemma_status, start_offset, end_offset, token_type, learning_score)
                VALUES (9104, 9102, 0, 'clear', 'clear', 'clear', 'FALLBACK', 0, 5, 'WORD', 1.0)
                """);
        jdbcTemplate.update("""
                INSERT INTO vocabulary(word, occurrences, occurrence_count, song_count, learning_score, recommended)
                VALUES ('clear', '[]', 1, 1, 1.0, 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO vocabulary_override(lemma, excluded, recommended_override, updated_at)
                VALUES ('clear', 0, 1, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO user_vocabulary(id, user_id, lemma, status, mastery_score, first_seen_at, last_seen_at)
                VALUES (9105, 'local', 'clear', 'LEARNING', 0.25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO vocabulary_occurrences(id, user_vocabulary_id, song_id, lyric_line_id, token_id)
                VALUES (9106, 9105, 9101, 9102, 9104)
                """);
        jdbcTemplate.update("""
                INSERT INTO phrase_occurrence(id, phrase_id, song_id, lyric_line_id, start_token_position,
                                              end_token_position, surface_phrase, dictionary_version,
                                              tokenizer_version, lemma_version)
                VALUES (9107, 1, 9101, 9102, 0, 0, 'clear', 'test', 'test', 'test')
                """);
        jdbcTemplate.update("""
                INSERT INTO phrase_cache_state(song_id, dictionary_version, tokenizer_version, lemma_version)
                VALUES (9101, 'test', 'test', 'test')
                """);
        jdbcTemplate.update("""
                INSERT INTO user_phrase(id, user_id, canonical_phrase, created_at)
                VALUES (9108, 'local', 'clear all', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("INSERT INTO app_meta(key, value) VALUES ('reset-test', 'keep')");
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
