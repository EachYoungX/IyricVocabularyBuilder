package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Normalizes metadata only for comparison; business fields keep their source spelling. */
@Component
public class MetadataComparisonNormalizer {
    public String normalizeTitle(String value) {
        return normalizePart(value);
    }

    public List<String> normalizeArtists(String value) {
        if (value == null) return List.of();
        return Arrays.stream(value.split("[/、,，＆&;；]+"))
                .map(this::normalizePart)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String normalizePart(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('—', '-')
                .replace('–', '-')
                .replace('－', '-');
        String previous;
        do {
            previous = normalized;
            normalized = normalized.replaceAll("\\([^()]*\\)", " ")
                    .replaceAll("（[^（）]*）", " ");
        } while (!normalized.equals(previous));
        return normalized.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
