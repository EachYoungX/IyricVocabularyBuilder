package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricLineType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LyricLineClassifier {
    private static final Pattern SECTION = Pattern.compile(
            "^\\[(verse|pre-?chorus|chorus|bridge|intro|outro|hook|refrain|interlude|post-?chorus)(?:\\s+[^]]+)?]$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SPEAKER = Pattern.compile("^[A-Z][A-Za-z'-]+(?:\\s+[A-Z][A-Za-z'-]+){0,3}:$");
    private static final Pattern META = Pattern.compile("^(produced|written|lyrics|music|composed|performed)\\s+by\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Set<String> PERFORMANCE_TERMS = Set.of(
            "guitar solo", "piano solo", "instrumental", "crowd cheering", "spoken", "applause"
    );

    public Classification classify(String normalizedText) {
        if (normalizedText.isEmpty()) return new Classification(LyricLineType.EMPTY, true, 1.0);
        if (SECTION.matcher(normalizedText).matches()) {
            return new Classification(LyricLineType.SECTION_LABEL, true, 0.99);
        }

        String semanticText = stripBrackets(normalizedText).toLowerCase(Locale.ROOT);
        if (PERFORMANCE_TERMS.contains(semanticText) || semanticText.endsWith(" solo")) {
            return new Classification(LyricLineType.PERFORMANCE_NOTE, true, 0.9);
        }
        if (META.matcher(normalizedText).matches()) {
            return new Classification(LyricLineType.META_INFO, true, 0.95);
        }
        if (normalizedText.length() <= 80 && SPEAKER.matcher(normalizedText).matches()) {
            return new Classification(LyricLineType.SPEAKER_LABEL, true, 0.92);
        }
        return new Classification(LyricLineType.LYRIC, false, 0.8);
    }

    private String stripBrackets(String text) {
        if (text.length() >= 2 && text.startsWith("[") && text.endsWith("]")) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    public record Classification(LyricLineType lineType, boolean hidden, double confidence) {}
}
