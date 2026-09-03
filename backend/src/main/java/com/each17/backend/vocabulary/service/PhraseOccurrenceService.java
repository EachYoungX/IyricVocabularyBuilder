package com.each17.backend.vocabulary.service;

import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dictionary.service.DictionaryMetadataRepository;
import com.each17.backend.dictionary.service.PhraseRepository;
import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.dto.PhraseOccurrenceDto;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.service.LyricNormalizer;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhraseOccurrenceService {
    private static final String TOKENIZER_VERSION = "1";
    private final Object cacheLock = new Object();
    private volatile boolean allSongsCacheReady;

    @Qualifier("appJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;
    private final DictionaryMetadataRepository metadataRepository;
    private final LyricLineRepository lyricLineRepository;
    private final LyricTokenRepository lyricTokenRepository;
    private final PhraseMatcher phraseMatcher;
    private final PhraseRepository phraseRepository;
    private final SongRepository songRepository;

    public PhraseOccurrenceService(
            @Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
            DictionaryMetadataRepository metadataRepository,
            LyricLineRepository lyricLineRepository,
            LyricTokenRepository lyricTokenRepository,
            PhraseMatcher phraseMatcher,
            PhraseRepository phraseRepository,
            SongRepository songRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRepository = metadataRepository;
        this.lyricLineRepository = lyricLineRepository;
        this.lyricTokenRepository = lyricTokenRepository;
        this.phraseMatcher = phraseMatcher;
        this.phraseRepository = phraseRepository;
        this.songRepository = songRepository;
    }

    @PostConstruct
    public void invalidateStaleCache() {
        jdbcTemplate.update("DELETE FROM phrase_occurrence WHERE dictionary_version <> ? OR tokenizer_version <> ? OR lemma_version <> ?",
                dictionaryVersion(), TOKENIZER_VERSION, lemmaVersion());
        allSongsCacheReady = false;
    }

    @Transactional
    public void refreshSong(Long songId) {
        List<LyricLine> lines = lyricLineRepository.findBySongIdOrderByLineIndexAsc(songId);
        jdbcTemplate.update("DELETE FROM phrase_occurrence WHERE song_id = ?", songId);
        for (LyricLine line : lines) {
            List<LyricToken> tokens = lyricTokenRepository.findByLyricLineIdOrderByTokenPositionAsc(line.getId());
            for (PhraseMatchDto match : phraseMatcher.findMatches(tokens)) {
                jdbcTemplate.update("""
                        INSERT OR IGNORE INTO phrase_occurrence(
                            phrase_id, song_id, lyric_line_id, start_token_position,
                            end_token_position, surface_phrase, dictionary_version,
                            tokenizer_version, lemma_version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, match.getPhraseId(), songId, line.getId(), match.getStartTokenPosition(),
                        match.getEndTokenPosition(), match.getSurfacePhrase(), dictionaryVersion(),
                        TOKENIZER_VERSION, lemmaVersion());
            }
        }
    }

    @Transactional
    public void invalidateSong(Long songId) {
        jdbcTemplate.update("DELETE FROM phrase_occurrence WHERE song_id = ?", songId);
        allSongsCacheReady = false;
    }

    @Transactional
    public List<PhraseMatchDto> findSongMatches(Long songId) {
        List<CachedMatch> cached = jdbcTemplate.query("""
                SELECT phrase_id, lyric_line_id, start_token_position, end_token_position, surface_phrase
                FROM phrase_occurrence
                WHERE song_id = ? AND dictionary_version = ? AND tokenizer_version = ? AND lemma_version = ?
                ORDER BY lyric_line_id, start_token_position, end_token_position
                """, (rs, rowNum) -> new CachedMatch(rs.getLong("phrase_id"), rs.getLong("lyric_line_id"),
                rs.getInt("start_token_position"), rs.getInt("end_token_position"), rs.getString("surface_phrase")),
                songId, dictionaryVersion(), TOKENIZER_VERSION, lemmaVersion());
        if (cached.isEmpty()) {
            refreshSong(songId);
            cached = jdbcTemplate.query("""
                    SELECT phrase_id, lyric_line_id, start_token_position, end_token_position, surface_phrase
                    FROM phrase_occurrence WHERE song_id = ?
                    ORDER BY lyric_line_id, start_token_position, end_token_position
                    """, (rs, rowNum) -> new CachedMatch(rs.getLong("phrase_id"), rs.getLong("lyric_line_id"),
                    rs.getInt("start_token_position"), rs.getInt("end_token_position"), rs.getString("surface_phrase")), songId);
        }
        Map<Long, PhraseEntry> entries = phraseRepository.findByIds(cached.stream().map(CachedMatch::phraseId).distinct().toList())
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
        ensureAllSongsCached();
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT phrase_id
                FROM phrase_occurrence
                WHERE dictionary_version = ?
                  AND tokenizer_version = ?
                  AND lemma_version = ?
                """, Long.class, dictionaryVersion(), TOKENIZER_VERSION, lemmaVersion());
    }

    @Transactional
    public List<PhraseOccurrenceDto> findPhraseOccurrences(Long phraseId) {
        ensureAllSongsCached();
        return queryPhraseOccurrences(phraseId);
    }

    private void ensureAllSongsCached() {
        if (allSongsCacheReady) return;
        synchronized (cacheLock) {
            if (allSongsCacheReady) return;
            for (Song song : songRepository.findAll()) refreshSong(song.getId());
            allSongsCacheReady = true;
        }
    }

    private List<PhraseOccurrenceDto> queryPhraseOccurrences(Long phraseId) {
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
                ORDER BY song.id ASC, line.line_index ASC, occurrence.start_token_position ASC
                """, (rs, rowNum) -> PhraseOccurrenceDto.builder()
                .phraseId(rs.getLong("phrase_id"))
                .songId(rs.getLong("song_id"))
                .songTitle(rs.getString("title"))
                .songArtist(rs.getString("artist"))
                .lyricLineId(rs.getLong("lyric_line_id"))
                .lineIndex(rs.getInt("line_index"))
                .lyricLine(LyricNormalizer.removeTimestamps(rs.getString("normalized_text")))
                .startTokenPosition(rs.getInt("start_token_position"))
                .endTokenPosition(rs.getInt("end_token_position"))
                .surfacePhrase(rs.getString("surface_phrase"))
                .build(), phraseId, dictionaryVersion(), TOKENIZER_VERSION, lemmaVersion());
    }

    private String dictionaryVersion() {
        return String.join(":", metadataRepository.find("package.version"), metadataRepository.find("schema.version"),
                metadataRepository.find("pattern.compiler.version"));
    }

    private String lemmaVersion() {
        return metadataRepository.find("lemma.rules.version");
    }

    private record CachedMatch(Long phraseId, Long lineId, int start, int end, String surfacePhrase) {
    }
}
