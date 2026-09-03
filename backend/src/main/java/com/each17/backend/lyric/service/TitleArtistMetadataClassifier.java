package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/** Identifies a timed title-artist header without deleting ordinary title lyrics. */
@Component
public class TitleArtistMetadataClassifier {
    public boolean matches(String line, String title, String artist) {
        if (line == null || title == null || artist == null
                || line.isBlank() || title.isBlank() || artist.isBlank()) return false;

        String[] parts = normalize(line).split("\\s+-\\s+", 2);
        if (parts.length != 2) return false;
        return normalize(parts[0]).equals(normalize(title))
                && normalize(parts[1]).equals(normalize(artist));
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('—', '-')
                .replace('–', '-')
                .replace('－', '-');
        normalized = normalized.replaceAll("\\([^)]*\\)", " ");
        return normalized.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*/\\s*", "/");
    }
}
