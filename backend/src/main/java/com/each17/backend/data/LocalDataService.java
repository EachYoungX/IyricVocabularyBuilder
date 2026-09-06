package com.each17.backend.data;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocalDataService {
    private static final List<String> USER_AND_DERIVED_TABLES = List.of(
            "phrase_occurrence",
            "phrase_cache_state",
            "vocabulary_occurrences",
            "song_credit",
            "lyric_tokens",
            "lyric_lines",
            "songs",
            "user_phrase",
            "user_vocabulary",
            "vocabulary_override",
            "vocabulary"
    );

    private final JdbcTemplate jdbcTemplate;

    public LocalDataService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void clearAll() {
        for (String table : USER_AND_DERIVED_TABLES) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }
}
