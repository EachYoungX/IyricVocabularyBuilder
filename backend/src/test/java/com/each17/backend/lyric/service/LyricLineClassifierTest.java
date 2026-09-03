package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricClassificationSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricLineClassifierTest {
    private final LyricLineClassifier classifier = new LyricLineClassifier();

    @ParameterizedTest
    @CsvSource({
            "'[Verse 1]', SECTION_LABEL, true",
            "'Taylor:', SPEAKER_LABEL, true",
            "'Guitar Solo', PERFORMANCE_NOTE, true",
            "'[Spoken]', PERFORMANCE_NOTE, true",
            "'Produced by Someone', CREDIT, true",
            "'I miss you', LYRIC, false",
            "'', EMPTY, true"
    })
    void classifiesLinesWithoutDeletingThem(String text, LyricLineType expectedType, boolean expectedHidden) {
        var result = classifier.classify(text);
        assertEquals(expectedType, result.lineType());
        assertEquals(expectedHidden, result.hidden());
    }

    @Test
    void classifiesHeaderCreditsButStopsCreditRulesAfterOrdinaryLyrics() {
        var result = classifier.classifyLines(List.of(
                new LyricNormalizer.NormalizedLine("Producer:Alec Justice", "Producer:Alec Justice", false),
                new LyricNormalizer.NormalizedLine("Mixing Engineer:Alec Justice", "Mixing Engineer:Alec Justice", false),
                new LyricNormalizer.NormalizedLine("I'm the producer of my own life", "I'm the producer of my own life", false),
                new LyricNormalizer.NormalizedLine("The guitar cries tonight", "The guitar cries tonight", false),
                new LyricNormalizer.NormalizedLine("Producer:Still a lyric", "Producer:Still a lyric", false)
        ));

        assertEquals(LyricLineType.CREDIT, result.get(0).lineType());
        assertEquals(LyricLineType.CREDIT, result.get(1).lineType());
        assertEquals(LyricLineType.LYRIC, result.get(2).lineType());
        assertEquals(LyricLineType.LYRIC, result.get(4).lineType());
        assertEquals(LyricClassificationSource.RULE, result.get(0).source());
    }

    @Test
    void recognizesInstrumentAndStudioCreditsInHeader() {
        var result = classifier.classifyLines(List.of(
                new LyricNormalizer.NormalizedLine("Piano:Someone", "Piano:Someone", false),
                new LyricNormalizer.NormalizedLine("Strings:Someone", "Strings:Someone", false),
                new LyricNormalizer.NormalizedLine("1st Violin:Someone", "1st Violin:Someone", false),
                new LyricNormalizer.NormalizedLine("Recording Assistant Engineer:Someone", "Recording Assistant Engineer:Someone", false),
                new LyricNormalizer.NormalizedLine("I miss you", "I miss you", false)
        ));

        assertEquals(List.of(LyricLineType.CREDIT, LyricLineType.CREDIT, LyricLineType.CREDIT,
                LyricLineType.CREDIT, LyricLineType.LYRIC), result.stream()
                .map(LyricLineClassifier.Classification::lineType).toList());
    }

    @Test
    void classifiesFormatMetadataAndSectionLabels() {
        var metadata = classifier.classify("[ar:Artist]", true, true);
        var section = classifier.classify("Chorus:", false, true);

        assertEquals(LyricLineType.METADATA, metadata.lineType());
        assertEquals(LyricClassificationSource.FORMAT, metadata.source());
        assertEquals(LyricLineType.SECTION_LABEL, section.lineType());
    }

    @Test
    void classifiesTimedTitleArtistLineAsMetadataOnlyWhenItMatchesSongMetadata() {
        var result = classifier.classifyLines(List.of(
                new LyricNormalizer.NormalizedLine(
                        "[00:00.00]The Other Side Of Paradise (Explicit) - Glass Animals",
                        "The Other Side Of Paradise (Explicit) - Glass Animals", false),
                new LyricNormalizer.NormalizedLine("[00:03.00]The other side", "The other side", false)
        ), "The Other Side Of Paradise", "Glass Animals");

        assertEquals(LyricLineType.METADATA, result.getFirst().lineType());
        assertEquals(LyricLineType.LYRIC, result.get(1).lineType());
    }

    @Test
    void ignoresTranslatedParentheticalsWhenMatchingTitleArtistHeader() {
        var result = classifier.classifyLines(List.of(
                new LyricNormalizer.NormalizedLine(
                        "[00:00.00]There For You (为你在此) - Martin Garrix (马丁·盖瑞斯)/Troye Sivan (特洛耶·希文)",
                        "There For You (为你在此) - Martin Garrix (马丁·盖瑞斯)/Troye Sivan (特洛耶·希文)", false),
                new LyricNormalizer.NormalizedLine("[00:01.00]I woke up pissed off today", "I woke up pissed off today", false)
        ), "There For You", "Martin Garrix/Troye Sivan");

        assertEquals(LyricLineType.METADATA, result.getFirst().lineType());
        assertEquals(LyricLineType.LYRIC, result.get(1).lineType());
    }

    @Test
    void keepsARepeatedSongTitleAsLyricsWhenItIsNotATitleArtistHeader() {
        var result = classifier.classifyLines(List.of(
                new LyricNormalizer.NormalizedLine("There For You", "There For You", false),
                new LyricNormalizer.NormalizedLine("There For You again tonight", "There For You again tonight", false)
        ), "There For You", "Martin Garrix/Troye Sivan");

        assertEquals(List.of(LyricLineType.LYRIC, LyricLineType.LYRIC), result.stream()
                .map(LyricLineClassifier.Classification::lineType).toList());
    }
}
