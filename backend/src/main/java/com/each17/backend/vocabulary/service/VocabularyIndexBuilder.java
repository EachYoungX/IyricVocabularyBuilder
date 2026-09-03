package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.service.LearningValuePolicy;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.song.entity.Song;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyIndexBuilder {
    private final LyricLineRepository lyricLineRepository;
    private final LyricTokenRepository lyricTokenRepository;
    private final LyricTokenizationService tokenizationService;
    private final LearningValuePolicy learningValuePolicy;
    private final ObjectMapper objectMapper;

    public List<Vocabulary> rebuildFromSongs(List<Song> songs) {
        Map<String, LemmaIndex> index = new HashMap<>();

        Map<Long, List<LyricLine>> linesBySongId = loadLinesBySongId(songs);

        for (Song song : songs) {
            List<LyricLine> lines = linesBySongId.getOrDefault(song.getId(), fallbackLines(song));
            for (LyricLine line : lines.stream().filter(this::shouldIndexLine).toList()) {
                List<LyricToken> lineTokens = line.getId() == null
                        ? List.of()
                        : lyricTokenRepository.findByLyricLineIdOrderByTokenPositionAsc(line.getId());
                if (lineTokens.isEmpty()) {
                    lineTokens = tokenizationService.tokenize(line);
                    if (line.getId() != null && !lineTokens.isEmpty()) lyricTokenRepository.saveAll(lineTokens);
                }
                for (LyricToken token : lineTokens) {
                    index.computeIfAbsent(token.getLemma(), ignored -> new LemmaIndex())
                            .add(token, line, song);
                }
            }
        }

        return index.entrySet().stream()
                .map(this::toVocabulary)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, List<LyricLine>> loadLinesBySongId(List<Song> songs) {
        List<Long> songIds = songs.stream()
                .map(Song::getId)
                .filter(Objects::nonNull)
                .toList();
        if (songIds.isEmpty()) return Map.of();

        return lyricLineRepository.findBySongIdsOrderBySongAndLineIndex(songIds).stream()
                .collect(Collectors.groupingBy(
                        line -> line.getSong().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(Function.identity(), Collectors.toList())
                ));
    }

    private boolean shouldIndexLine(LyricLine line) {
        return line.getLineType() == LyricLineType.LYRIC || line.getLineType() == LyricLineType.UNKNOWN;
    }

    private Vocabulary toVocabulary(Map.Entry<String, LemmaIndex> entry) {
        try {
            LemmaIndex value = entry.getValue();
            return Vocabulary.builder()
                    .word(entry.getKey())
                    .occurrences(objectMapper.writeValueAsString(value.occurrences()))
                    .displayForms(objectMapper.writeValueAsString(value.displayForms()))
                    .occurrenceCount(value.occurrences().size())
                    .songCount(value.songIds().size())
                    .learningScore(value.learningScore())
                    .recommended(learningValuePolicy.recommended(value.learningScore()))
                    .build();
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<LyricLine> fallbackLines(Song song) {
        String lyrics = song.getNormalizedLyrics() != null ? song.getNormalizedLyrics()
                : (song.getRawLyrics() != null ? song.getRawLyrics() : song.getLyrics());
        if (lyrics == null) return List.of();

        String[] splitLines = lyrics.split("\\R");
        List<LyricLine> lines = new ArrayList<>();
        for (int i = 0; i < splitLines.length; i++) {
            lines.add(LyricLine.builder()
                    .song(song)
                    .lineIndex(i)
                    .originalText(splitLines[i])
                    .normalizedText(splitLines[i])
                    .lineType(LyricLineType.LYRIC)
                    .hidden(false)
                    .confidence(0.5)
                    .userOverride(false)
                    .build());
        }
        return lines;
    }

    private static final class LemmaIndex {
        private final List<WordOccurrenceDto> occurrences = new ArrayList<>();
        private final Set<String> displayForms = new TreeSet<>();
        private final Set<Long> songIds = new HashSet<>();
        private double learningScore = 0.0;

        void add(LyricToken token, LyricLine line, Song song) {
            displayForms.add(token.getSurfaceForm().toLowerCase());
            songIds.add(song.getId());
            learningScore = Math.max(learningScore, token.getLearningScore());
            occurrences.add(WordOccurrenceDto.builder()
                    .songId(song.getId())
                    .songTitle(song.getTitle())
                    .songArtist(song.getArtist())
                    .lyricLineId(line.getId())
                    .lineIndex(line.getLineIndex())
                    .lyricLine(line.getNormalizedText())
                    .tokenPosition(token.getTokenPosition())
                    .surfaceForm(token.getSurfaceForm())
                    .normalizedForm(token.getNormalizedForm())
                    .lemma(token.getLemma())
                    .lemmaStatus(token.getLemmaStatus())
                    .tokenType(token.getTokenType())
                    .startOffset(token.getStartOffset())
                    .endOffset(token.getEndOffset())
                    .learningScore(token.getLearningScore())
                    .build());
        }

        List<WordOccurrenceDto> occurrences() {
            return occurrences;
        }

        Set<String> displayForms() {
            return displayForms;
        }

        Set<Long> songIds() {
            return songIds;
        }

        double learningScore() {
            return learningScore;
        }
    }
}
