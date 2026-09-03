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
        assertEquals("[00:12.50]  Ｈｅｌｌｏ   world  ", result.lines().getFirst().originalText());
    }

    @Test
    void removesLrcMetadataAndTitleCreditLines() {
        var result = normalizer.normalize("[ti:Demo]\n[ar:Artist]\n[00:00]Demo - Artist\n[00:01]Hello world");

        assertEquals("Hello world", result.text());
        assertEquals(1, result.lines().size());
    }
}
