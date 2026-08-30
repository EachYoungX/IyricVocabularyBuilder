package com.each17.backend.lyric.service;

import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.entity.LyricTokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LyricTokenizationService {
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");

    private final EnglishLemmaService lemmaService;
    private final LearningValuePolicy learningValuePolicy;

    public List<LyricToken> tokenize(LyricLine line) {
        List<LyricToken> tokens = new ArrayList<>();
        if (line == null || line.getNormalizedText() == null || Boolean.TRUE.equals(line.getHidden())) {
            return tokens;
        }

        Matcher matcher = WORD_PATTERN.matcher(line.getNormalizedText());
        int tokenPosition = 0;
        while (matcher.find()) {
            String surface = matcher.group();
            String normalized = normalize(surface);
            if (normalized.isBlank()) continue;

            EnglishLemmaService.LemmaResolution lemmaResolution = lemmaService.resolve(normalized);
            double score = learningValuePolicy.score(normalized, lemmaResolution.lemma());
            LyricTokenType type = score < 0.5 ? LyricTokenType.LOW_VALUE
                    : normalized.contains("'") ? LyricTokenType.CONTRACTION : LyricTokenType.WORD;
            tokens.add(LyricToken.builder()
                    .lyricLine(line)
                    .tokenPosition(tokenPosition++)
                    .surfaceForm(surface)
                    .normalizedForm(normalized)
                    .lemma(lemmaResolution.lemma())
                    .lemmaStatus(lemmaResolution.status())
                    .startOffset(matcher.start())
                    .endOffset(matcher.end())
                    .tokenType(type)
                    .learningScore(score)
                    .build());
        }
        return tokens;
    }

    public String normalize(String surface) {
        if (surface == null) return "";
        return surface.toLowerCase()
                .replace("’", "'")
                .replaceAll("^'+|'+$", "");
    }

    public String normalizeToLemmaPhrase(String value) {
        String normalizedPhrase = normalizePhrase(value);
        if (normalizedPhrase.contains(" ")) {
            return normalizedPhrase;
        }
        return lemmaService.lemma(normalizedPhrase);
    }

    /**
     * Normalizes a user-entered phrase without applying word lemma rules to each
     * part. Phrase canonicalization belongs to the phrase dictionary layer.
     */
    public String normalizePhrase(String value) {
        if (value == null) return "";
        return Arrays.stream(value.split("\\s+"))
                .map(this::normalize)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
