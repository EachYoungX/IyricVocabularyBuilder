package com.each17.backend.lyric.service;

import com.each17.backend.common.exception.DictionaryNotFoundException;
import com.each17.backend.dictionary.service.DictionaryService;
import com.each17.backend.lyric.entity.LyricLemmaStatus;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EnglishLemmaServiceTest {
    private final EnglishLemmaService service = new EnglishLemmaService();

    @Test
    void mapsCommonInflectionsToLemma() {
        assertEquals("run", service.lemma("running"));
        assertEquals("leave", service.lemma("leaving"));
        assertEquals("run", service.lemma("ran"));
        assertEquals("run", service.lemma("runs"));
        assertEquals("try", service.lemma("tries"));
        assertEquals("make", service.lemma("made"));
        assertNotEquals("leav", service.lemma("leaving"));
    }

    @Test
    void fallsBackToSurfaceFormWhenDictionaryRejectsCandidates() {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        when(dictionaryService.lookupWord("leav")).thenThrow(new DictionaryNotFoundException("leav"));
        when(dictionaryService.lookupWord("leave")).thenThrow(new DictionaryNotFoundException("leave"));

        EnglishLemmaService dictionaryBackedService = new EnglishLemmaService(dictionaryService);

        assertEquals("leaving", dictionaryBackedService.lemma("leaving"));
        assertEquals(LyricLemmaStatus.FALLBACK, dictionaryBackedService.resolve("leaving").status());
    }

    @Test
    void reportsVerifiedAndUnknownResolutionStates() {
        assertEquals(LyricLemmaStatus.VERIFIED, service.resolve("running").status());
        assertEquals(LyricLemmaStatus.UNKNOWN, service.resolve("").status());
    }
}
