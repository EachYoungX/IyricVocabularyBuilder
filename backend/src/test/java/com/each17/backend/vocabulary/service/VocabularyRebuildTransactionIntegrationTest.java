package com.each17.backend.vocabulary.service;

import com.each17.backend.vocabulary.entity.Vocabulary;
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
class VocabularyRebuildTransactionIntegrationTest {
    @Autowired
    private VocabularyRebuildTransaction rebuildTransaction;

    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM vocabulary_override");
        jdbcTemplate.update("DELETE FROM vocabulary");
        jdbcTemplate.update("""
                INSERT INTO vocabulary(word, occurrences, occurrence_count, song_count, learning_score, recommended)
                VALUES ('old', '[]', 1, 1, 1.0, 1)
                """);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_vocabulary_insert");
        jdbcTemplate.update("DELETE FROM vocabulary_override");
        jdbcTemplate.update("DELETE FROM vocabulary");
    }

    @Test
    void failedReplacementRollsBackDelete() {
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_vocabulary_insert BEFORE INSERT ON vocabulary
                BEGIN SELECT RAISE(ABORT, 'forced rebuild failure'); END
                """);
        Vocabulary generated = Vocabulary.builder().word("new").occurrences("[]")
                .occurrenceCount(1).songCount(1).learningScore(1.0).recommended(true).build();

        assertThrows(RuntimeException.class, () -> rebuildTransaction.replace(List.of(generated)));

        assertEquals(List.of("old"), jdbcTemplate.queryForList("SELECT word FROM vocabulary", String.class));
    }

    @Test
    void appliesPersistentExclusionAndRecommendedOverride() {
        jdbcTemplate.update("""
                INSERT INTO vocabulary_override(lemma, excluded, recommended_override, updated_at)
                VALUES ('skip', 1, 0, CURRENT_TIMESTAMP), ('keep', 0, 0, CURRENT_TIMESTAMP)
                """);
        Vocabulary skip = Vocabulary.builder().word("skip").occurrences("[]")
                .occurrenceCount(1).songCount(1).learningScore(1.0).recommended(true).build();
        Vocabulary keep = Vocabulary.builder().word("keep").occurrences("[]")
                .occurrenceCount(1).songCount(1).learningScore(1.0).recommended(true).build();

        rebuildTransaction.replace(List.of(skip, keep));

        assertEquals(List.of("keep"), jdbcTemplate.queryForList("SELECT word FROM vocabulary", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT recommended FROM vocabulary WHERE word = 'keep'", Integer.class));
    }
}
