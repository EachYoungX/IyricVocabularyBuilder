package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PhraseOccurrenceCacheWriter {
    private final JdbcTemplate jdbcTemplate;
    private final LyricLineRepository lyricLineRepository;
    private final LyricTokenRepository lyricTokenRepository;
    private final PhraseMatcher phraseMatcher;

    public PhraseOccurrenceCacheWriter(
            @Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
            LyricLineRepository lyricLineRepository,
            LyricTokenRepository lyricTokenRepository,
            PhraseMatcher phraseMatcher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.lyricLineRepository = lyricLineRepository;
        this.lyricTokenRepository = lyricTokenRepository;
        this.phraseMatcher = phraseMatcher;
    }

    @Transactional
    public void refreshSong(Long songId, PhraseCacheVersion version) {
        List<LyricLine> lines = lyricLineRepository.findBySongIdOrderByLineIndexAsc(songId);
        List<Long> lineIds = lines.stream().map(LyricLine::getId).toList();
        Map<Long, List<LyricToken>> tokensByLine = lineIds.isEmpty() ? Map.of()
                : lyricTokenRepository.findByLyricLineIdsOrderByLineAndPosition(lineIds).stream()
                .collect(Collectors.groupingBy(token -> token.getLyricLine().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        jdbcTemplate.update("DELETE FROM phrase_occurrence WHERE song_id = ?", songId);
        for (LyricLine line : lines) {
            for (PhraseMatchDto match : phraseMatcher.findMatches(tokensByLine.getOrDefault(line.getId(), List.of()))) {
                jdbcTemplate.update("""
                        INSERT OR IGNORE INTO phrase_occurrence(
                            phrase_id, song_id, lyric_line_id, start_token_position,
                            end_token_position, surface_phrase, dictionary_version,
                            tokenizer_version, lemma_version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, match.getPhraseId(), songId, line.getId(), match.getStartTokenPosition(),
                        match.getEndTokenPosition(), match.getSurfacePhrase(), version.dictionary(),
                        version.tokenizer(), version.lemma());
            }
        }
        jdbcTemplate.update("""
                INSERT OR REPLACE INTO phrase_cache_state(
                    song_id, dictionary_version, tokenizer_version, lemma_version, built_at
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, songId, version.dictionary(), version.tokenizer(), version.lemma());
    }
}
