package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricLineType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricLineClassifierTest {
    private final LyricLineClassifier classifier = new LyricLineClassifier();

    @ParameterizedTest
    @CsvSource({
            "'[Verse 1]', SECTION_LABEL, true",
            "'Taylor:', SPEAKER_LABEL, true",
            "'Guitar Solo', PERFORMANCE_NOTE, true",
            "'[Spoken]', PERFORMANCE_NOTE, true",
            "'Produced by Someone', META_INFO, true",
            "'I miss you', LYRIC, false",
            "'', EMPTY, true"
    })
    void classifiesLinesWithoutDeletingThem(String text, LyricLineType expectedType, boolean expectedHidden) {
        var result = classifier.classify(text);
        assertEquals(expectedType, result.lineType());
        assertEquals(expectedHidden, result.hidden());
    }
}
