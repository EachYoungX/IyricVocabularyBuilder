package com.each17.backend.vocabulary.service;

import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dictionary.service.DictionaryMetadataRepository;
import com.each17.backend.dictionary.service.PhraseRepository;
import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.dto.PhraseOccurrenceDto;
import com.each17.backend.lyric.service.LyricNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhraseOccurrenceService {
    private static final String TOKENIZER_VERSION = "1";
    private static final String LINE_CLASSIFIER_VERSION = "9";
    private static final String PHRASE_MATCHER_VERSION = "4";
    private final Object cacheLock = new Object();

    private final JdbcTemplate jdbcTemplate;
    private final DictionaryMetadataRepository metadataRepository;
    private final PhraseRepository phraseRepository;
    private final PhraseOccurrenceCacheWriter cacheWriter;
    private volatile PhraseCacheVersion cachedVersion;

    @Value("${app.dictionary.enabled:false}")
    private boolean dictionaryEnabled = true;

    public PhraseOccurrenceService(
            @Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
            DictionaryMetadataRepository metadataRepository,
            PhraseRepository phraseRepository,
            PhraseOccurrenceCacheWriter cacheWriter
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRepository = metadataRepository;
        this.phraseRepository = phraseRepository;
        this.cacheWriter = cacheWriter;
    }

    @PostConstruct
    public void invalidateStaleCache() {
        if (!dictionaryEnabled) {
            jdbcTemplate.update("DELETE FROM phrase_occurrence");
            jdbcTemplate.update("DELETE FROM phrase_cache_state");
            return;
        }
        PhraseCacheVersion version = version();
        jdbcTemplate.update("""
                DELETE FROM phrase_occurrence
                WHERE dictionary_version <> ? OR tokenizer_version <> ? OR lemma_version <> ?
                """, version.dictionary(), version.tokenizer(), version.lemma());
        jdbcTemplate.update("""
                DELETE FROM phrase_cache_state
                WHERE dictionary_version <> ? OR tokenizer_version <> ? OR lemma_version <> ?
                """, version.dictionary(), version.tokenizer(), version.lemma());
    }

    public void refreshSong(Long songId) {
        if (!dictionaryEnabled) {
            invalidateSong(songId);
            return;
        }
        cacheWriter.refreshSong(songId, version());
    }

    @Transactional
    public void invalidateSong(Long songId) {
        jdbcTemplate.update("DELETE FROM phrase_occurrence WHERE song_id = ?", songId);
        jdbcTemplate.update("DELETE FROM phrase_cache_state WHERE song_id = ?", songId);
    }

    @Transactional
    public void invalidateAll() {
        jdbcTemplate.update("DELETE FROM phrase_occurrence");
        jdbcTemplate.update("DELETE FROM phrase_cache_state");
    }

    public List<PhraseMatchDto> findSongMatches(Long songId) {
        if (!dictionaryEnabled) return List.of();
        PhraseCacheVersion version = version();
        ensureSongCached(songId, version);
        List<CachedMatch> cached = queryCachedMatches(songId, version);
        Map<Long, PhraseEntry> entries = phraseRepository.findByIds(
                        cached.stream().map(CachedMatch::phraseId).distinct().toList())
                .stream().collect(Collectors.toMap(PhraseEntry::id, Function.identity()));
        List<PhraseMatchDto> result = new ArrayList<>();
        for (CachedMatch item : cached) {
            PhraseEntry entry = entries.get(item.phraseId());
            if (entry == null) continue;
            result.add(PhraseMatchDto.builder()
                    .phraseId(entry.id()).sourcePattern(entry.sourcePattern()).canonicalPattern(entry.canonicalPattern())
                    .definitionEn(entry.definitionEn()).definitionZh(entry.definitionZh()).usageNoteZh(entry.usageNoteZh())
                    .phraseType(entry.phraseType()).source(entry.source())
                    .startTokenPosition(item.start()).endTokenPosition(item.end())
                    .surfacePhrase(item.surfacePhrase()).matchPriority(entry.matchPriority()).build());
        }
        return result;
    }

    public List<Long> findMatchedPhraseIds() {
        if (!dictionaryEnabled) return List.of();
        PhraseCacheVersion version = version();
        ensureAllSongsCached(version);
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT phrase_id FROM phrase_occurrence
                WHERE dictionary_version = ? AND tokenizer_version = ? AND lemma_version = ?
                """, Long.class, version.dictionary(), version.tokenizer(), version.lemma());
    }

    public List<PhraseOccurrenceDto> findPhraseOccurrences(Long phraseId) {
        if (!dictionaryEnabled) return List.of();
        PhraseCacheVersion version = version();
        ensureAllSongsCached(version);
        return queryPhraseOccurrences(phraseId, version);
    }

    private void ensureSongCached(Long songId, PhraseCacheVersion version) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM phrase_cache_state
                WHERE song_id = ? AND dictionary_version = ? AND tokenizer_version = ? AND lemma_version = ?
                """, Integer.class, songId, version.dictionary(), version.tokenizer(), version.lemma());
        if (count == null || count == 0) cacheWriter.refreshSong(songId, version);
    }

    private void ensureAllSongsCached(PhraseCacheVersion version) {
        synchronized (cacheLock) {
            Set<Long> built = new HashSet<>(jdbcTemplate.queryForList("""
                    SELECT song_id FROM phrase_cache_state
                    WHERE dictionary_version = ? AND tokenizer_version = ? AND lemma_version = ?
                    """, Long.class, version.dictionary(), version.tokenizer(), version.lemma()));
            List<Long> missing = jdbcTemplate.queryForList("SELECT id FROM songs ORDER BY id", Long.class).stream()
                    .filter(id -> !built.contains(id)).toList();
            for (Long songId : missing) cacheWriter.refreshSong(songId, version);
        }
    }

    private List<CachedMatch> queryCachedMatches(Long songId, PhraseCacheVersion version) {
        return jdbcTemplate.query("""
                SELECT phrase_id, lyric_line_id, start_token_position, end_token_position, surface_phrase
                FROM phrase_occurrence
                WHERE song_id = ? AND dictionary_version = ? AND tokenizer_version = ? AND lemma_version = ?
                ORDER BY lyric_line_id, start_token_position, end_token_position
                """, (rs, rowNum) -> new CachedMatch(rs.getLong("phrase_id"), rs.getLong("lyric_line_id"),
                rs.getInt("start_token_position"), rs.getInt("end_token_position"), rs.getString("surface_phrase")),
                songId, version.dictionary(), version.tokenizer(), version.lemma());
    }

    private List<PhraseOccurrenceDto> queryPhraseOccurrences(Long phraseId, PhraseCacheVersion version) {
        return jdbcTemplate.query("""
                SELECT occurrence.phrase_id, occurrence.song_id, song.title, song.artist,
                       occurrence.lyric_line_id, line.line_index, line.normalized_text,
                       occurrence.start_token_position, occurrence.end_token_position,
                       occurrence.surface_phrase
                FROM phrase_occurrence occurrence
                JOIN songs song ON song.id = occurrence.song_id
                JOIN lyric_lines line ON line.id = occurrence.lyric_line_id
                WHERE occurrence.phrase_id = ?
                  AND occurrence.dictionary_version = ?
                  AND occurrence.tokenizer_version = ?
                  AND occurrence.lemma_version = ?
                ORDER BY song.id, line.line_index, occurrence.start_token_position
                """, (rs, rowNum) -> PhraseOccurrenceDto.builder()
                .phraseId(rs.getLong("phrase_id")).songId(rs.getLong("song_id"))
                .songTitle(rs.getString("title")).songArtist(rs.getString("artist"))
                .lyricLineId(rs.getLong("lyric_line_id")).lineIndex(rs.getInt("line_index"))
                .lyricLine(LyricNormalizer.removeTimestamps(rs.getString("normalized_text")))
                .startTokenPosition(rs.getInt("start_token_position"))
                .endTokenPosition(rs.getInt("end_token_position"))
                .surfacePhrase(rs.getString("surface_phrase")).build(),
                phraseId, version.dictionary(), version.tokenizer(), version.lemma());
    }

    private PhraseCacheVersion version() {
        PhraseCacheVersion existing = cachedVersion;
        if (existing != null) return existing;
        Map<String, String> metadata = metadataRepository.findAll();
        cachedVersion = new PhraseCacheVersion(String.join(":",
                value(metadata, "package.version"), value(metadata, "schema.version"),
                value(metadata, "pattern.compiler.version"), "line-classifier-" + LINE_CLASSIFIER_VERSION,
                "phrase-matcher-" + PHRASE_MATCHER_VERSION), TOKENIZER_VERSION,
                value(metadata, "lemma.rules.version"));
        return cachedVersion;
    }

    private String value(Map<String, String> metadata, String key) {
        return metadata.getOrDefault(key, "unknown");
    }

    private record CachedMatch(Long phraseId, Long lineId, int start, int end, String surfacePhrase) {
    }
}
