package com.each17.backend.vocabulary.service;

import com.each17.backend.dictionary.model.PhraseAnchor;
import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dictionary.model.PhrasePatternToken;
import com.each17.backend.dictionary.service.PhraseAnchorRepository;
import com.each17.backend.dictionary.service.PhrasePatternRepository;
import com.each17.backend.dictionary.service.PhraseRepository;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.entity.LyricLemmaStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class PhraseMatcherTest {
    private final PhraseAnchorRepository anchors = mock(PhraseAnchorRepository.class);
    private final PhraseRepository phrases = mock(PhraseRepository.class);
    private final PhrasePatternRepository patterns = mock(PhrasePatternRepository.class);
    private final PhraseMatcher matcher = new PhraseMatcher(anchors, phrases, patterns);

    @Test
    void matchesLemmaCanonicalPhraseWithoutGeneratingItFromSurfaceForms() {
        when(anchors.findByTokens(anyCollection()))
                .thenAnswer(invocation -> invocation.<Collection<String>>getArgument(0).contains("eyes")
                        ? List.of(new PhraseAnchor(1L, 2, "NORMALIZED", "eyes")) : List.of());
        when(phrases.findByIds(anyCollection())).thenReturn(List.of(new PhraseEntry(1L, "be all eyes", "be all eyes",
                "watch closely", null, null, "FIXED_EXPRESSION", "2ndLA", 3, 3, 0)));
        when(patterns.findByPhraseIds(anyCollection())).thenReturn(List.of(
                literal(1L, 0, "LEMMA", "be"),
                literal(1L, 1, "NORMALIZED", "all"),
                literal(1L, 2, "NORMALIZED", "eyes")));

        var result = matcher.findMatches(List.of(token("we", "we", "we"), token("were", "were", "be"),
                token("all", "all", "all"), token("eyes", "eyes", "eye")), 3);

        assertEquals(1, result.size());
        assertEquals("were all eyes", result.getFirst().getSurfacePhrase());
        assertEquals("be all eyes", result.getFirst().getCanonicalPattern());
    }

    @Test
    void supportsBoundedGapPatternOnTheSameLine() {
        when(anchors.findByTokens(anyCollection()))
                .thenAnswer(invocation -> invocation.<Collection<String>>getArgument(0).contains("as")
                        ? List.of(new PhraseAnchor(2L, 0, "NORMALIZED", "as")) : List.of());
        when(phrases.findByIds(anyCollection())).thenReturn(List.of(new PhraseEntry(2L, "as...as", "as <GAP> as",
                "comparison", null, null, "PATTERN", "2ndLA", 3, 5, 0)));
        when(patterns.findByPhraseIds(anyCollection())).thenReturn(List.of(
                literal(2L, 0, "NORMALIZED", "as"),
                new PhrasePatternToken(2L, 1, "GAP", null, null, null, 1, 3),
                literal(2L, 2, "NORMALIZED", "as")));

        var result = matcher.findMatches(List.of(token("as", "as", "as"), token("beautiful", "beautiful", "beautiful"),
                token("as", "as", "as")), null);

        assertEquals(1, result.size());
        assertEquals("as beautiful as", result.getFirst().getSurfacePhrase());
    }

    @Test
    void rejectsNonPossessiveSpanForPossessiveSlot() {
        stubPossessivePhrase();

        var result = matcher.findMatches(List.of(
                token("Need", "need", "need"), token("you", "you", "you"), token("on", "on", "on"),
                token("the", "the", "the"), token("other", "other", "other"), token("side", "side", "side")
        ));

        assertEquals(0, result.size());
    }

    @Test
    void acceptsDeterminerAndApostropheSForPossessiveSlot() {
        stubPossessivePhrase();
        var yourSide = matcher.findMatches(List.of(
                token("I'm", "i'm", "i"), token("on", "on", "on"), token("your", "your", "your"),
                token("side", "side", "side")
        ));
        var johnsSide = matcher.findMatches(List.of(
                token("He", "he", "he"), token("stayed", "stayed", "stay"), token("on", "on", "on"),
                token("John's", "john's", "john"), token("side", "side", "side")
        ));

        assertEquals(1, yourSide.size());
        assertEquals("on your side", yourSide.getFirst().getSurfacePhrase());
        assertEquals(1, johnsSide.size());
        assertEquals("on John's side", johnsSide.getFirst().getSurfacePhrase());
    }

    private void stubPossessivePhrase() {
        when(anchors.findByTokens(anyCollection()))
                .thenAnswer(invocation -> invocation.<Collection<String>>getArgument(0).contains("on")
                        ? List.of(new PhraseAnchor(3L, 0, "NORMALIZED", "on")) : List.of());
        when(phrases.findByIds(anyCollection())).thenReturn(List.of(new PhraseEntry(3L, "on one's side",
                "on <POSSESSIVE> side", "support", null, null, "PATTERN", "2ndLA", 3, 5, 0)));
        when(patterns.findByPhraseIds(anyCollection())).thenReturn(List.of(
                literal(3L, 0, "NORMALIZED", "on"),
                new PhrasePatternToken(3L, 1, "SLOT", null, null, "POSSESSIVE", 1, 3),
                literal(3L, 2, "NORMALIZED", "side")
        ));
    }

    private PhrasePatternToken literal(long phraseId, int position, String matchType, String value) {
        return new PhrasePatternToken(phraseId, position, "LITERAL", matchType, value, null, 1, 1);
    }

    private LyricToken token(String surface, String normalized, String lemma) {
        return LyricToken.builder().surfaceForm(surface).normalizedForm(normalized).lemma(lemma)
                .lemmaStatus(LyricLemmaStatus.VERIFIED).build();
    }
}
