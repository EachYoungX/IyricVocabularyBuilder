package com.each17.backend.dictionary.service;

import com.each17.backend.dictionary.model.PhraseAnchor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PhraseAnchorRepository {
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public PhraseAnchorRepository(@Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PhraseAnchor> findByToken(String normalized, String lemma, String surface) {
        return jdbcTemplate.query("""
                SELECT phrase_id, anchor_position, anchor_type, anchor_value
                FROM phrase_anchor
                WHERE (anchor_type = 'NORMALIZED' AND anchor_value IN (?, ?))
                   OR (anchor_type = 'LEMMA' AND anchor_value IN (?, ?))
                   OR (anchor_type = 'SURFACE' AND anchor_value = ?)
                """, (rs, rowNum) -> new PhraseAnchor(rs.getLong("phrase_id"),
                rs.getInt("anchor_position"), rs.getString("anchor_type"),
                rs.getString("anchor_value")), normalized, lemma, normalized, lemma, surface);
    }
}
