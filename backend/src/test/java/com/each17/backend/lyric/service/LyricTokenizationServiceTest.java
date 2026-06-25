package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricTokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyricTokenizationServiceTest {
    private final EnglishLemmaService lemmaService = new EnglishLemmaService();
    private final LearningValuePolicy policy = new LearningValuePolicy();
    private final LyricTokenizationService service = new LyricTokenizationService(lemmaService, policy);

    @Test
    void emitsOffsetsLemmaAndLearningScore() {
        LyricLine line = LyricLine.builder()
                .normalizedText("I was running away, yeah")
                .lineType(LyricLineType.LYRIC)
                .hidden(false)
                .build();

        var tokens = service.tokenize(line);

        assertEquals(5, tokens.size());
        assertEquals("running", tokens.get(2).getSurfaceForm());
        assertEquals("run", tokens.get(2).getLemma());
        assertEquals(6, tokens.get(2).getStartOffset());
        assertEquals(13, tokens.get(2).getEndOffset());
        assertEquals(LyricTokenType.LOW_VALUE, tokens.get(4).getTokenType());
        assertTrue(tokens.get(2).getLearningScore() > tokens.get(4).getLearningScore());
    }

    @Test
    void skipsHiddenLines() {
        LyricLine line = LyricLine.builder()
                .normalizedText("Hidden words")
                .hidden(true)
                .build();

        assertTrue(service.tokenize(line).isEmpty());
    }
}
