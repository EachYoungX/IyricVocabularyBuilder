package com.each17.backend.dictionary.service;

import com.each17.backend.dictionary.model.PhrasePatternToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PhrasePatternRepository {
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public PhrasePatternRepository(@Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PhrasePatternToken> findByPhraseId(long phraseId) {
        return jdbcTemplate.query("""
                SELECT phrase_id, pattern_position, token_type, match_type, match_value,
                       slot_hint, min_tokens, max_tokens
                FROM phrase_pattern_token
                WHERE phrase_id = ?
                ORDER BY pattern_position ASC
                """, (rs, rowNum) -> new PhrasePatternToken(
                rs.getLong("phrase_id"), rs.getInt("pattern_position"),
                rs.getString("token_type"), rs.getString("match_type"),
                rs.getString("match_value"), rs.getString("slot_hint"),
                rs.getInt("min_tokens"), rs.getInt("max_tokens")), phraseId);
    }
}
