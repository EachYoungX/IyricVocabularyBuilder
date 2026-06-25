package com.each17.backend.lyric.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnglishLemmaServiceTest {
    private final EnglishLemmaService service = new EnglishLemmaService();

    @Test
    void mapsCommonInflectionsToLemma() {
        assertEquals("run", service.lemma("running"));
        assertEquals("run", service.lemma("ran"));
        assertEquals("run", service.lemma("runs"));
        assertEquals("try", service.lemma("tries"));
        assertEquals("make", service.lemma("made"));
    }
}
