package com.each17.backend.lyric.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricNormalizerTest {
    private final LyricNormalizer normalizer = new LyricNormalizer();

    @Test
    void normalizesLrcWhitespaceFullWidthCharactersAndRepeatedEmptyLines() {
        var result = normalizer.normalize("[ti:Demo]\r\n[ar:Artist]\r\n[00:12.50]  Ｈｅｌｌｏ   world  \r\n\r\n\r\n[00:15]Again\r\n");

        assertEquals("Hello world\n\nAgain", result.text());
        assertEquals(5, result.lines().size());
        assertEquals("[ti:Demo]", result.lines().getFirst().originalText());
        assertEquals("Hello world", result.lines().get(2).normalizedText());
        assertEquals("[ar:Artist]", result.lines().get(1).originalText());
    }

    @Test
    void removesLrcMetadataAndTitleCreditLines() {
        var result = normalizer.normalize("[ti:Demo]\n[ar:Artist]\n[00:00]Demo - Artist\n[00:01]Hello world");

        assertEquals("Hello world", result.text());
        assertEquals(3, result.lines().size());
        assertEquals("[ti:Demo]", result.lines().getFirst().originalText());
        assertEquals("Hello world", result.lines().getLast().normalizedText());
    }

    @Test
    void removesLeadingTimestampsFromLegacyDisplayText() {
        assertEquals("I'm running", LyricNormalizer.removeTimestamps("[02:44.16]I'm running"));
        assertEquals("Keep this", LyricNormalizer.removeTimestamps("[00:01][00:02.500]Keep this"));
    }
}
