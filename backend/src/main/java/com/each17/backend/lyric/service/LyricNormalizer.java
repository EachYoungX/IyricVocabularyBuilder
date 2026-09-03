package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LyricNormalizer {
    private static final Pattern LRC_TIMESTAMP = Pattern.compile("^(?:\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?])+\\s*");
    private static final Pattern LRC_METADATA = Pattern.compile("^\\[([A-Za-z][A-Za-z0-9_-]*):(.*)]\\s*$");
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[\\t \\x0B\\f]+");

    public NormalizedLyrics normalize(String rawLyrics) {
        String canonical = rawLyrics == null ? "" : rawLyrics.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = canonical.split("\\n", -1);
        List<NormalizedLine> lines = new ArrayList<>();
        Map<String, String> metadata = new LinkedHashMap<>();
        boolean previousEmpty = false;

        for (String rawLine : rawLines) {
            var metadataMatcher = LRC_METADATA.matcher(rawLine);
            if (metadataMatcher.matches()) {
                metadata.put(metadataMatcher.group(1).toLowerCase(Locale.ROOT), metadataMatcher.group(2).trim());
                previousEmpty = false;
                continue;
            }
            String normalized = normalizeLine(rawLine);
            boolean empty = normalized.isEmpty();
            if (empty && previousEmpty) continue;
            lines.add(new NormalizedLine(rawLine, normalized, false));
            previousEmpty = empty;
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).normalizedText().isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        String normalizedText = String.join("\n", lines.stream()
                .filter(line -> !line.formatMetadata())
                .map(NormalizedLine::normalizedText).toList());
        return new NormalizedLyrics(normalizedText, List.copyOf(lines), Map.copyOf(metadata));
    }

    public static String removeTimestamps(String line) {
        if (line == null) return "";
        return LRC_TIMESTAMP.matcher(line).replaceFirst("");
    }

    public String normalizeLineForStorage(String line) {
        return normalizeLine(line == null ? "" : line);
    }

    private String normalizeLine(String line) {
        String withoutTimestamp = removeTimestamps(line);
        String withoutEscapedColon = withoutTimestamp.replace("\\:", ":");
        String unicodeNormalized = Normalizer.normalize(withoutEscapedColon, Normalizer.Form.NFKC);
        return HORIZONTAL_WHITESPACE.matcher(unicodeNormalized).replaceAll(" ").trim();
    }

    public record NormalizedLyrics(String text, List<NormalizedLine> lines, Map<String, String> metadata) {
        public NormalizedLyrics(String text, List<NormalizedLine> lines) {
            this(text, lines, Map.of());
        }
    }

    public static boolean isFormatMetadata(String line) {
        return line != null && LRC_METADATA.matcher(line).matches();
    }

    public record NormalizedLine(String originalText, String normalizedText, boolean formatMetadata) {}
}
