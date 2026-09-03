package com.each17.backend.dictionary.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class DictionaryMetadataRepository {
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public DictionaryMetadataRepository(@Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, String> findAll() {
        try {
            return jdbcTemplate.query("SELECT key, value FROM dictionary_meta ORDER BY key",
                    rows -> {
                        Map<String, String> result = new LinkedHashMap<>();
                        while (rows.next()) {
                            result.put(rows.getString("key"), rows.getString("value"));
                        }
                        return result;
                    });
        } catch (DataAccessException exception) {
            return Map.of();
        }
    }

    public String find(String key) {
        try {
            return jdbcTemplate.query("SELECT value FROM dictionary_meta WHERE key = ?",
                    ps -> ps.setString(1, key),
                    rows -> rows.next() ? rows.getString("value") : null);
        } catch (DataAccessException exception) {
            return null;
        }
    }
}
