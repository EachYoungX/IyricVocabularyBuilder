package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SectionLabelClassifier {
    private static final Pattern SECTION = Pattern.compile(
            "^\\s*(?:\\[(?:verse|pre-?chorus|chorus|bridge|intro|outro|hook|refrain|interlude|post-?chorus)(?:\\s+[^]]+)?]|(?:verse|pre-?chorus|chorus|bridge|intro|outro|hook|refrain|interlude|post-?chorus)\\s*:)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isSectionLabel(String text) {
        return text != null && SECTION.matcher(text).matches();
    }
}
