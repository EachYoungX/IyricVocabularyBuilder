package com.each17.backend.dictionary.service;

import com.each17.backend.common.exception.DictionaryNotFoundException;
import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DictionaryServiceImpl implements DictionaryService {
    private static final int MAX_CACHE_SIZE = 2_000;

    private final JdbcTemplate jdbcTemplate;
    private final DictionaryMetadataRepository metadataRepository;
    private final Map<String, DictionaryEntryDto> lookupCache = new ConcurrentHashMap<>();

    @Value("${app.dictionary.enabled:true}")
    private boolean enabled = true;

    public DictionaryServiceImpl(@Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, null);
    }

    @Autowired
    public DictionaryServiceImpl(
            @Qualifier("dictionaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            DictionaryMetadataRepository metadataRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRepository = metadataRepository;
    }

    private static final RowMapper<DictionaryEntryDto> ROW_MAPPER = new DictionaryEntryRowMapper();

    @Override
    public DictionaryEntryDto lookupWord(String word) {
        if (!enabled) throw new DictionaryNotFoundException(word);
        String normalizedWord = normalizeWord(word);
        DictionaryEntryDto cached = lookupCache.get(normalizedWord);
        if (cached != null) return cached;
        DictionaryEntryDto entry = queryDictionary(normalizedWord);
        cache(normalizedWord, entry);
        return entry;
    }

    @Override
    public Optional<DictionaryEntryDto> findWord(String word) {
        if (!enabled || word == null || word.isBlank()) return Optional.empty();
        String normalizedWord = normalizeWord(word);
        DictionaryEntryDto cached = lookupCache.get(normalizedWord);
        if (cached != null) return Optional.of(cached);
        try {
            DictionaryEntryDto entry = queryDictionary(normalizedWord);
            cache(normalizedWord, entry);
            return Optional.of(entry);
        } catch (DictionaryNotFoundException exception) {
            return Optional.empty();
        }
    }

    private DictionaryEntryDto queryDictionary(String normalizedWord) {
        String sql = """
                SELECT word, phonetic,
                       definition_en AS definition,
                       translation_zh AS translation,
                       pos_profile AS pos,
                       collins_star,
                       oxford_core,
                       tags,
                       bnc_rank AS bnc,
                       bnc_rank AS bnc_rank,
                       coca_rank,
                       coca_rank AS frq,
                       coca_rank AS frq_rank,
                       morphology AS forms
                FROM word_entry
                WHERE word = ? COLLATE NOCASE
                """;
        try {
            return jdbcTemplate.queryForObject(sql, ROW_MAPPER, normalizedWord);
        } catch (EmptyResultDataAccessException exception) {
            throw new DictionaryNotFoundException(normalizedWord);
        }
    }

    private void cache(String word, DictionaryEntryDto entry) {
        if (lookupCache.size() >= MAX_CACHE_SIZE) lookupCache.clear();
        lookupCache.put(word, entry);
    }

    private String normalizeWord(String word) {
        if (word == null) throw new DictionaryNotFoundException("");
        String normalized = word.trim().toLowerCase();
        if (normalized.isBlank()) throw new DictionaryNotFoundException(word);
        return normalized;
    }

    @Override
    public List<DictionarySourceDto> getSourceInfo() {
        if (!enabled) {
            return List.of(DictionarySourceDto.builder()
                    .sourceName("NONE")
                    .dictionaryVersion("disabled")
                    .requiresAttribution(false)
                    .commercialUseAllowed(false)
                    .redistributionAllowed(false)
                    .attributionText("No dictionary is configured for this runtime.")
                    .build());
        }
        Map<String, String> meta = metadataRepository == null ? Map.of() : metadataRepository.findAll();
        String importedAt = meta.getOrDefault("build.time", "unknown");
        return List.of(
                DictionarySourceDto.builder()
                        .sourceName("ECDICT")
                        .sourceUrl("https://github.com/skywind3000/ECDICT")
                        .dictionaryVersion(meta.getOrDefault("ecdict.commit", "unknown"))
                        .importedAt(importedAt)
                        .licenseName("MIT License")
                        .requiresAttribution(true)
                        .commercialUseAllowed(true)
                        .redistributionAllowed(true)
                        .attributionText("Word dictionary source used for word lookup.")
                        .build(),
                DictionarySourceDto.builder()
                        .sourceName("2ndLA/english-phrases")
                        .sourceUrl("https://github.com/2ndLA/english-phrases")
                        .dictionaryVersion(meta.getOrDefault("2ndla.commit", "unknown"))
                        .importedAt(importedAt)
                        .licenseName("CC BY-SA 4.0")
                        .requiresAttribution(true)
                        .commercialUseAllowed(true)
                        .redistributionAllowed(true)
                        .attributionText("Phrase dictionary source used for phrase matching.")
                        .build()
        );
    }

    private static final class DictionaryEntryRowMapper implements RowMapper<DictionaryEntryDto> {
        @Override
        public DictionaryEntryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            int bnc = rs.getInt("bnc");
            if (bnc == 0 || rs.wasNull()) bnc = rs.getInt("bnc_rank");
            int frq = rs.getInt("frq");
            if (frq == 0 || rs.wasNull()) frq = rs.getInt("frq_rank");
            return DictionaryEntryDto.builder()
                    .word(rs.getString("word"))
                    .phonetic(rs.getString("phonetic"))
                    .definition(rs.getString("definition"))
                    .translation(rs.getString("translation"))
                    .pos(rs.getString("pos"))
                    .collins(rs.getInt("collins_star"))
                    .oxford(rs.getInt("oxford_core"))
                    .tags(rs.getString("tags"))
                    .bnc(bnc)
                    .coca(rs.getInt("coca_rank"))
                    .frq(frq)
                    .forms(rs.getString("forms"))
                    .build();
        }
    }
}
