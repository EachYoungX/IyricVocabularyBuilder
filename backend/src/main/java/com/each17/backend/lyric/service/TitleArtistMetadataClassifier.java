package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/** Identifies a timed title-artist header without deleting ordinary title lyrics. */
@Component
public class TitleArtistMetadataClassifier {
    private final MetadataComparisonNormalizer normalizer;

    public TitleArtistMetadataClassifier() {
        this(new MetadataComparisonNormalizer());
    }

    @Autowired
    public TitleArtistMetadataClassifier(MetadataComparisonNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public boolean matches(String line, String title, String artist) {
        if (line == null || title == null || artist == null
                || line.isBlank() || title.isBlank() || artist.isBlank()) return false;

        String[] parts = line.replace('—', '-').replace('–', '-').replace('－', '-').split("\\s+-\\s+", 2);
        if (parts.length != 2) return false;
        return normalizer.normalizeTitle(parts[0]).equals(normalizer.normalizeTitle(title))
                && artistsEquivalent(parts[1], artist);
    }

    private boolean artistsEquivalent(String left, String right) {
        List<String> leftArtists = normalizer.normalizeArtists(left);
        List<String> rightArtists = normalizer.normalizeArtists(right);
        return !leftArtists.isEmpty() && leftArtists.equals(rightArtists);
    }
}
