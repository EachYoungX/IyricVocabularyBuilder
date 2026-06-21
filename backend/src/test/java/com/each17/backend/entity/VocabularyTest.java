package com.each17.backend.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VocabularyTest {

    @Test
    void testVocabularyBuilder() {
        String occurrencesJson = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        
        Vocabulary vocabulary = Vocabulary.builder()
                .word("yesterday")
                .occurrences(occurrencesJson)
                .build();

        assertEquals("yesterday", vocabulary.getWord());
        assertEquals(occurrencesJson, vocabulary.getOccurrences());
    }

    @Test
    void testVocabularySettersAndGetters() {
        String occurrencesJson = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWord("yesterday");
        vocabulary.setOccurrences(occurrencesJson);

        assertEquals("yesterday", vocabulary.getWord());
        assertEquals(occurrencesJson, vocabulary.getOccurrences());
    }
}