package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricClassificationSource;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LyricLineClassifier {
    private static final int MAX_HEADER_CANDIDATE_LINES = 20;
    private static final Pattern SPEAKER = Pattern.compile("^[A-Z][A-Za-z'-]+(?:\\s+[A-Z][A-Za-z'-]+){0,3}:$");
    private static final Set<String> PERFORMANCE_TERMS = Set.of(
            "guitar solo", "piano solo", "instrumental", "crowd cheering", "spoken", "applause"
    );

    private final SectionLabelClassifier sectionLabelClassifier;
    private final CreditLineClassifier creditLineClassifier;
    private final TitleArtistMetadataClassifier titleArtistMetadataClassifier;

    public LyricLineClassifier() {
        this(new SectionLabelClassifier(), new CreditLineClassifier(), new TitleArtistMetadataClassifier());
    }

    @Autowired
    public LyricLineClassifier(SectionLabelClassifier sectionLabelClassifier, CreditLineClassifier creditLineClassifier) {
        this(sectionLabelClassifier, creditLineClassifier, new TitleArtistMetadataClassifier());
    }

    public LyricLineClassifier(SectionLabelClassifier sectionLabelClassifier, CreditLineClassifier creditLineClassifier,
                               TitleArtistMetadataClassifier titleArtistMetadataClassifier) {
        this.sectionLabelClassifier = sectionLabelClassifier;
        this.creditLineClassifier = creditLineClassifier;
        this.titleArtistMetadataClassifier = titleArtistMetadataClassifier;
    }

    public Classification classify(String normalizedText) {
        return classify(normalizedText, false, true);
    }

    public Classification classify(String normalizedText, boolean formatMetadata, boolean headerMode) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return new Classification(LyricLineType.EMPTY, LyricClassificationSource.RULE, true, 1.0);
        }
        if (formatMetadata) {
            return new Classification(LyricLineType.METADATA, LyricClassificationSource.FORMAT, true, 1.0);
        }
        if (sectionLabelClassifier.isSectionLabel(normalizedText)) {
            return new Classification(LyricLineType.SECTION_LABEL, LyricClassificationSource.RULE, true, 0.99);
        }

        String semanticText = stripBrackets(normalizedText).toLowerCase(Locale.ROOT);
        if (PERFORMANCE_TERMS.contains(semanticText) || semanticText.endsWith(" solo")) {
            return new Classification(LyricLineType.PERFORMANCE_NOTE, LyricClassificationSource.RULE, true, 0.9);
        }
        if (headerMode && creditLineClassifier.isCredit(normalizedText)) {
            return new Classification(LyricLineType.CREDIT, LyricClassificationSource.RULE, true, 0.98);
        }
        if (normalizedText.length() <= 80 && SPEAKER.matcher(normalizedText).matches()) {
            return new Classification(LyricLineType.SPEAKER_LABEL, LyricClassificationSource.RULE, true, 0.92);
        }
        return new Classification(LyricLineType.LYRIC, LyricClassificationSource.DEFAULT, false, 0.8);
    }

    public List<Classification> classifyLines(List<LyricNormalizer.NormalizedLine> lines) {
        return classifyLines(lines, null, null);
    }

    public List<Classification> classifyLines(List<LyricNormalizer.NormalizedLine> lines, String title, String artist) {
        List<Classification> classifications = new ArrayList<>();
        boolean headerMode = true;
        int ordinaryLyricLines = 0;
        int nonEmptyCandidateLines = 0;
        for (LyricNormalizer.NormalizedLine line : lines) {
            Classification classification = classify(line.normalizedText(), line.formatMetadata(), headerMode, title, artist);
            classifications.add(classification);
            if (classification.lineType() != LyricLineType.EMPTY) {
                nonEmptyCandidateLines++;
                if (nonEmptyCandidateLines >= MAX_HEADER_CANDIDATE_LINES) headerMode = false;
            }
            if (classification.lineType() == LyricLineType.LYRIC && hasOrdinaryLyricStructure(line.normalizedText())) {
                ordinaryLyricLines++;
                if (ordinaryLyricLines >= 2) headerMode = false;
            } else if (classification.lineType() != LyricLineType.EMPTY) {
                ordinaryLyricLines = 0;
            }
        }
        return List.copyOf(classifications);
    }

    private Classification classify(String normalizedText, boolean formatMetadata, boolean headerMode,
                                    String title, String artist) {
        Classification base = classify(normalizedText, formatMetadata, headerMode);
        if (base.lineType() == LyricLineType.LYRIC && headerMode
                && titleArtistMetadataClassifier.matches(normalizedText, title, artist)) {
            return new Classification(LyricLineType.METADATA, LyricClassificationSource.RULE, true, 0.97);
        }
        return base;
    }

    public static boolean isIndexableLine(LyricLine line) {
        return line != null && line.getLineType() == LyricLineType.LYRIC;
    }

    private boolean hasOrdinaryLyricStructure(String text) {
        return text != null && text.matches(".*[A-Za-z].*");
    }

    private String stripBrackets(String text) {
        if (text.length() >= 2 && text.startsWith("[") && text.endsWith("]")) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    public record Classification(
            LyricLineType lineType,
            LyricClassificationSource source,
            boolean hidden,
            double confidence
    ) {}
}
