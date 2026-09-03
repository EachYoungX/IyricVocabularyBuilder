package com.each17.backend.lyric.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricNormalizerTest {
    private final LyricNormalizer normalizer = new LyricNormalizer();

    @Test
    void normalizesLrcWhitespaceFullWidthCharactersAndRepeatedEmptyLines() {
        var result = normalizer.normalize("[ti:Demo]\r\n[ar:Artist]\r\n[00:12.50]  Ｈｅｌｌｏ   world  \r\n\r\n\r\n[00:15]Again\r\n");

        assertEquals("Hello world\n\nAgain", result.text());
        assertEquals(3, result.lines().size());
        assertEquals("Demo", result.metadata().get("ti"));
        assertEquals("Artist", result.metadata().get("ar"));
        assertEquals("Hello world", result.lines().getFirst().normalizedText());
    }

    @Test
    void removesLrcMetadataAndTitleCreditLines() {
        var result = normalizer.normalize("[ti:Demo]\n[ar:Artist]\n[00:00]Demo - Artist\n[00:01]Hello world");

        assertEquals("Demo - Artist\nHello world", result.text());
        assertEquals(2, result.lines().size());
        assertEquals("Demo", result.metadata().get("ti"));
        assertEquals("Hello world", result.lines().getLast().normalizedText());
    }

    @Test
    void removesLeadingTimestampsFromLegacyDisplayText() {
        assertEquals("I'm running", LyricNormalizer.removeTimestamps("[02:44.16]I'm running"));
        assertEquals("Keep this", LyricNormalizer.removeTimestamps("[00:01][00:02.500]Keep this"));
    }

    @Test
    void removesAccidentalEscapingFromNormalizedBusinessText() {
        assertEquals("Produced by:Lil Silva", normalizer.normalizeLineForStorage("Produced by\\:Lil Silva"));
    }
}
