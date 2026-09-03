package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class LyricNormalizer {
    private static final Pattern LRC_TIMESTAMP = Pattern.compile("^(?:\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?])+\\s*");
    private static final Pattern LRC_METADATA = Pattern.compile("^\\[[A-Za-z][A-Za-z0-9_-]*:.*\\]\\s*$");
    private static final Pattern LRC_TITLE_CREDIT = Pattern.compile("^.+\\s+-\\s+.+$");
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[\\t \\x0B\\f]+" );

    public NormalizedLyrics normalize(String rawLyrics) {
        String canonical = rawLyrics == null ? "" : rawLyrics.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = canonical.split("\n", -1);
        List<NormalizedLine> lines = new ArrayList<>();
        boolean previousEmpty = false;

        for (String rawLine : rawLines) {
            if (LRC_METADATA.matcher(rawLine).matches()) continue;
            String normalized = normalizeLine(rawLine);
            if (LRC_TIMESTAMP.matcher(rawLine).find() && LRC_TITLE_CREDIT.matcher(normalized).matches()) continue;
            boolean empty = normalized.isEmpty();
            if (empty && previousEmpty) continue;
            lines.add(new NormalizedLine(rawLine, normalized));
            previousEmpty = empty;
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).normalizedText().isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        String normalizedText = String.join("\n", lines.stream().map(NormalizedLine::normalizedText).toList());
        return new NormalizedLyrics(normalizedText, List.copyOf(lines));
    }

    public static String removeTimestamps(String line) {
        if (line == null) return "";
        return LRC_TIMESTAMP.matcher(line).replaceFirst("");
    }

    private String normalizeLine(String line) {
        String withoutTimestamp = removeTimestamps(line);
        String unicodeNormalized = Normalizer.normalize(withoutTimestamp, Normalizer.Form.NFKC);
        return HORIZONTAL_WHITESPACE.matcher(unicodeNormalized).replaceAll(" ").trim();
    }

    public record NormalizedLyrics(String text, List<NormalizedLine> lines) {}
    public record NormalizedLine(String originalText, String normalizedText) {}
}
