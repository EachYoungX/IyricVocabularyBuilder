package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LearningValuePolicy;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.song.entity.Song;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.each17.backend.dto.WordOccurrenceDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class VocabularyIndexBuilderTest {
    private final LyricLineRepository lyricLineRepository = mock(LyricLineRepository.class);
    private final LyricTokenRepository lyricTokenRepository = mock(LyricTokenRepository.class);
    private final LearningValuePolicy policy = new LearningValuePolicy();
    private final LyricTokenizationService tokenizationService = new LyricTokenizationService(new EnglishLemmaService(), policy);
    private final VocabularyIndexBuilder builder = new VocabularyIndexBuilder(
            lyricLineRepository, lyricTokenRepository, tokenizationService, policy, new ObjectMapper()
    );

    @Test
    void batchesLineLoadingAndAggregatesByLemma() {
        Song song = Song.builder().id(1L).title("Run Song").artist("Artist").lyrics("running").build();
        LyricLine line = LyricLine.builder()
                .id(10L)
                .song(song)
                .lineIndex(0)
                .normalizedText("I was running and ran")
                .lineType(LyricLineType.LYRIC)
                .hidden(false)
                .build();
        when(lyricLineRepository.findBySongIdsOrderBySongAndLineIndex(anyCollection())).thenReturn(List.of(line));

        var result = builder.rebuildFromSongs(List.of(song));

        assertTrue(result.stream().anyMatch(vocabulary -> vocabulary.getWord().equals("run")
                && vocabulary.getOccurrenceCount() == 2
                && vocabulary.getRecommended()));
        verify(lyricLineRepository, times(1)).findBySongIdsOrderBySongAndLineIndex(List.of(1L));
        verify(lyricTokenRepository, never()).deleteAllInBatch();
        verify(lyricTokenRepository, times(1)).saveAll(argThat(tokens -> tokens.iterator().hasNext()));
    }

    @Test
    void fallsBackToSongLyricsWhenStructuredLinesAreMissing() {
        Song song = Song.builder()
                .id(2L)
                .title("Fallback Song")
                .artist("Artist")
                .normalizedLyrics("dreaming loudly")
                .build();
        when(lyricLineRepository.findBySongIdsOrderBySongAndLineIndex(anyCollection())).thenReturn(List.of());

        var result = builder.rebuildFromSongs(List.of(song));

        assertTrue(result.stream().anyMatch(vocabulary -> vocabulary.getWord().equals("dream")));
        verify(lyricTokenRepository, never()).saveAll(anyCollection());
    }

    @Test
    void doesNotIndexAdjacentWordsAsPhrases() throws Exception {
        Song song = Song.builder().id(3L).title("Phrase Song").artist("Artist").lyrics("blue sky").build();
        LyricLine line = LyricLine.builder()
                .id(30L)
                .song(song)
                .lineIndex(0)
                .normalizedText("blue sky")
                .lineType(LyricLineType.LYRIC)
                .hidden(false)
                .build();
        when(lyricLineRepository.findBySongIdsOrderBySongAndLineIndex(anyCollection())).thenReturn(List.of(line));

        var result = builder.rebuildFromSongs(List.of(song));

        assertTrue(result.stream().noneMatch(vocabulary -> vocabulary.getWord().contains(" ")));
        assertTrue(result.stream().anyMatch(vocabulary -> vocabulary.getWord().equals("blue")));
        assertTrue(result.stream().anyMatch(vocabulary -> vocabulary.getWord().equals("sky")));

        Vocabulary blue = result.stream().filter(vocabulary -> vocabulary.getWord().equals("blue")).findFirst().orElseThrow();
        var occurrences = new ObjectMapper().readValue(blue.getOccurrences(), new TypeReference<List<WordOccurrenceDto>>() {});
        assertEquals(0, occurrences.get(0).getTokenPosition());
        assertEquals("blue", occurrences.get(0).getNormalizedForm());
    }
}
