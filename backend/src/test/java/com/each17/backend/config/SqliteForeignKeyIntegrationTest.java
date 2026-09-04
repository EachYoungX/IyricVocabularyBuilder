package com.each17.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SqliteForeignKeyIntegrationTest {
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("appDataSource")
    private DataSource dataSource;

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM songs WHERE id = 901");
    }

    @Test
    void enablesForeignKeysOnEveryPooledConnectionAndCascadesSongData() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, result.getInt(1));
        }

        jdbcTemplate.update("INSERT INTO songs(id, title, artist, lyrics) VALUES (901, 'FK', 'Test', 'line')");
        jdbcTemplate.update("""
                INSERT INTO lyric_lines(id, song_id, line_index, original_text, normalized_text, line_type,
                                        classification_source, hidden, confidence, user_override)
                VALUES (902, 901, 0, 'line', 'line', 'LYRIC', 'DEFAULT', 0, 1.0, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO lyric_tokens(id, lyric_line_id, token_position, surface_form, normalized_form,
                                         lemma, lemma_status, start_offset, end_offset, token_type, learning_score)
                VALUES (903, 902, 0, 'line', 'line', 'line', 'VERIFIED', 0, 4, 'WORD', 1.0)
                """);
        jdbcTemplate.update("""
                INSERT INTO song_credit(id, song_id, credit_type, credit_value, sort_order)
                VALUES (904, 901, 'OTHER', 'credit', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO phrase_occurrence(id, phrase_id, song_id, lyric_line_id, start_token_position,
                                              end_token_position, surface_phrase, dictionary_version,
                                              tokenizer_version, lemma_version)
                VALUES (905, 1, 901, 902, 0, 0, 'line', '1', '1', '1')
                """);

        jdbcTemplate.update("DELETE FROM songs WHERE id = 901");

        assertEquals(0, count("lyric_lines", 902));
        assertEquals(0, count("lyric_tokens", 903));
        assertEquals(0, count("song_credit", 904));
        assertEquals(0, count("phrase_occurrence", 905));
    }

    private int count(String table, long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }
}
