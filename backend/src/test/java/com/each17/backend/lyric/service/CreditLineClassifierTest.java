package com.each17.backend.lyric.service;

import com.each17.backend.song.entity.SongCreditType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditLineClassifierTest {
    private final CreditLineClassifier classifier = new CreditLineClassifier();

    @ParameterizedTest
    @CsvSource({
            "'Lyrics by Alec Justice', LYRICIST, 'Alec Justice'",
            "'Composed by Troye Sivan', COMPOSER, 'Troye Sivan'",
            "'Produced by: Lil Silva', PRODUCER, 'Lil Silva'",
            "'Producer: Martin Garrix', PRODUCER, 'Martin Garrix'",
            "'Mixing Engineer: John Smith', MIXING_ENGINEER, 'John Smith'",
            "'制作人：某某', PRODUCER, '某某'"
    })
    void parsesStructuredCreditRoles(String line, SongCreditType type, String value) {
        var result = classifier.parse(line);

        assertTrue(result.isPresent());
        assertEquals(type, result.get().creditType());
        assertEquals(value, result.get().creditValue());
    }
}
