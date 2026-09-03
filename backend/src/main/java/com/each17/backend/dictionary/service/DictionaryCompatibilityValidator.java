package com.each17.backend.dictionary.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
public class DictionaryCompatibilityValidator {
    private final JdbcTemplate jdbcTemplate;
    private final DictionaryMetadataRepository metadataRepository;
    private final boolean enabled;

    public DictionaryCompatibilityValidator(
            @Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            DictionaryMetadataRepository metadataRepository,
            @Value("${app.dictionary.enabled:true}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRepository = metadataRepository;
        this.enabled = enabled;
    }

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        Map<String, String> meta = metadataRepository.findAll();
        if (!"1".equals(meta.get("schema.version"))) {
            throw new IllegalStateException("Unsupported lyric dictionary schema.version: " + meta.get("schema.version"));
        }
        String integrity = jdbcTemplate.queryForObject("PRAGMA integrity_check", String.class);
        if (!"ok".equalsIgnoreCase(integrity)) {
            throw new IllegalStateException("Lyric dictionary integrity check failed: " + integrity);
        }
        for (String table : new String[]{"word_entry", "phrase_entry", "phrase_pattern_token", "phrase_anchor", "dictionary_meta"}) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = ?", Integer.class, table);
            if (count == null || count == 0) throw new IllegalStateException("Missing lyric dictionary table: " + table);
        }
    }
}
