package com.each17.backend.lyric.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyricsHashServiceTest {
    private final LyricsHashService hashService = new LyricsHashService();

    @Test
    void producesStableSha256Hash() {
        String first = hashService.hash("same lyrics");
        assertEquals(first, hashService.hash("same lyrics"));
        assertNotEquals(first, hashService.hash("different lyrics"));
        assertEquals(64, first.length());
    }
}
