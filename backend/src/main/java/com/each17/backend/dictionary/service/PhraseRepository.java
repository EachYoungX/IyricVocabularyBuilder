package com.each17.backend.dictionary.service;

import com.each17.backend.dictionary.model.PhraseEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class PhraseRepository {
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public PhraseRepository(@Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PhraseEntry> findById(long id) {
        List<PhraseEntry> entries = jdbcTemplate.query("""
                SELECT id, source_pattern, canonical_pattern, definition_en, definition_zh,
                       usage_note_zh, phrase_type, source, token_count_min, token_count_max,
                       match_priority
                FROM phrase_entry WHERE id = ?
                """, (rs, rowNum) -> map(rs), id);
        return entries.stream().findFirst();
    }

    public List<PhraseEntry> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(ignored -> "?").toList());
        return jdbcTemplate.query("""
                SELECT id, source_pattern, canonical_pattern, definition_en, definition_zh,
                       usage_note_zh, phrase_type, source, token_count_min, token_count_max,
                       match_priority
                FROM phrase_entry WHERE id IN (""" + placeholders + ")", (rs, rowNum) -> map(rs), ids.toArray());
    }

    public List<PhraseEntry> search(String query, int limit) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) return List.of();
        return searchPage(normalized, limit, 0);
    }

    public long count(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM phrase_entry", Long.class);
        }
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM phrase_entry
                WHERE canonical_pattern LIKE ? COLLATE NOCASE
                   OR source_pattern LIKE ? COLLATE NOCASE
                """, Long.class, "%" + normalized + "%", "%" + normalized + "%");
    }

    public List<PhraseEntry> searchPage(String query, int limit, int offset) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT id, source_pattern, canonical_pattern, definition_en, definition_zh,
                           usage_note_zh, phrase_type, source, token_count_min, token_count_max,
                           match_priority
                    FROM phrase_entry
                    ORDER BY match_priority DESC, token_count_max DESC, canonical_pattern ASC
                    LIMIT ? OFFSET ?
                    """, (rs, rowNum) -> map(rs), limit, offset);
        }
        return jdbcTemplate.query("""
                SELECT id, source_pattern, canonical_pattern, definition_en, definition_zh,
                       usage_note_zh, phrase_type, source, token_count_min, token_count_max,
                       match_priority
                FROM phrase_entry
                WHERE canonical_pattern LIKE ? COLLATE NOCASE
                   OR source_pattern LIKE ? COLLATE NOCASE
                ORDER BY match_priority DESC, token_count_max DESC, canonical_pattern ASC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> map(rs), "%" + normalized + "%", "%" + normalized + "%", limit, offset);
    }

    private PhraseEntry map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PhraseEntry(rs.getLong("id"), rs.getString("source_pattern"),
                rs.getString("canonical_pattern"), rs.getString("definition_en"),
                rs.getString("definition_zh"), rs.getString("usage_note_zh"),
                rs.getString("phrase_type"), rs.getString("source"),
                rs.getInt("token_count_min"), rs.getInt("token_count_max"),
                rs.getInt("match_priority"));
    }
}
