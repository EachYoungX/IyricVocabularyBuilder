package com.each17.backend.lyric.service;

import com.each17.backend.dictionary.service.DictionaryService;
import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.lyric.entity.LyricLemmaStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnglishLemmaService {
    private static final Map<String, String> IRREGULARS = Map.ofEntries(
            Map.entry("am", "be"), Map.entry("are", "be"), Map.entry("is", "be"), Map.entry("was", "be"), Map.entry("were", "be"),
            Map.entry("been", "be"), Map.entry("being", "be"),
            Map.entry("ran", "run"), Map.entry("running", "run"),
            Map.entry("sang", "sing"), Map.entry("sung", "sing"),
            Map.entry("went", "go"), Map.entry("gone", "go"),
            Map.entry("did", "do"), Map.entry("done", "do"),
            Map.entry("had", "have"), Map.entry("has", "have"),
            Map.entry("made", "make"), Map.entry("took", "take"), Map.entry("taken", "take"),
            Map.entry("came", "come"), Map.entry("saw", "see"), Map.entry("seen", "see"),
            Map.entry("said", "say"), Map.entry("got", "get"), Map.entry("gotten", "get")
    );

    /**
     * Used by lightweight unit tests and non-Spring callers. In the application,
     * candidate lemmas are checked against the bundled dictionary instead.
     */
    private static final Set<String> BUILT_IN_LEMMAS = Set.of(
            "be", "come", "do", "dream", "eye", "get", "give", "go", "have", "leave", "live", "look",
            "love", "make", "run", "say", "see", "sing", "study", "take", "try", "walk"
    );

    private final DictionaryService dictionaryService;
    private final Map<String, Boolean> validationCache = new ConcurrentHashMap<>();

    public EnglishLemmaService() {
        this(null);
    }

    @Autowired
    public EnglishLemmaService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public String lemma(String normalizedForm) {
        return resolve(normalizedForm).lemma();
    }

    public LemmaResolution resolve(String normalizedForm) {
        if (normalizedForm == null || normalizedForm.isBlank()) {
            return new LemmaResolution("", LyricLemmaStatus.UNKNOWN);
        }
        String word = normalizedForm.toLowerCase();

        Optional<DictionaryEntryDto> exactEntry = findDictionaryEntry(word);
        if (exactEntry.isPresent()) {
            Optional<String> morphologyLemma = morphologyLemma(exactEntry.get().getForms(), word);
            if (morphologyLemma.isPresent() && isTrustedLemma(morphologyLemma.get())) {
                return new LemmaResolution(morphologyLemma.get(), LyricLemmaStatus.VERIFIED);
            }
            return new LemmaResolution(word, LyricLemmaStatus.VERIFIED);
        }

        if (IRREGULARS.containsKey(word)) {
            String candidate = IRREGULARS.get(word);
            return isTrustedLemma(candidate)
                    ? new LemmaResolution(candidate, LyricLemmaStatus.VERIFIED)
                    : new LemmaResolution(word, LyricLemmaStatus.FALLBACK);
        }
        if (word.length() <= 3) {
            return new LemmaResolution(word, trustedOrFallback(word));
        }

        if (word.endsWith("'s") || word.endsWith("s'")) {
            word = word.substring(0, word.length() - 2);
        }

        if (word.endsWith("ies") && word.length() > 4) {
            return resolveCandidates(word, List.of(word.substring(0, word.length() - 3) + "y"));
        }
        if (word.endsWith("ied") && word.length() > 4) {
            return resolveCandidates(word, List.of(word.substring(0, word.length() - 3) + "y"));
        }
        if (word.endsWith("ing") && word.length() > 5) {
            String stem = word.substring(0, word.length() - 3);
            if (hasDoubledFinalConsonant(stem)) stem = stem.substring(0, stem.length() - 1);
            if (stem.length() > 2 && !stem.endsWith("e")) {
                return resolveCandidates(word, List.of(stem, stem + "e"));
            }
        }
        if (word.endsWith("ed") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 2);
            if (hasDoubledFinalConsonant(stem)) stem = stem.substring(0, stem.length() - 1);
            if (stem.endsWith("i")) {
                return resolveCandidates(word, List.of(stem.substring(0, stem.length() - 1) + "y"));
            }
            return resolveCandidates(word, List.of(stem, stem + "e"));
        }
        if (word.endsWith("es") && word.length() > 4 && (word.endsWith("ses") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("ches") || word.endsWith("shes"))) {
            return resolveCandidates(word, List.of(word.substring(0, word.length() - 2)));
        }
        if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 3) {
            return resolveCandidates(word, List.of(word.substring(0, word.length() - 1)));
        }
        return new LemmaResolution(word, trustedOrFallback(word));
    }

    private LemmaResolution resolveCandidates(String original, List<String> candidates) {
        for (String candidate : candidates) {
            if (isTrustedLemma(candidate)) {
                return new LemmaResolution(candidate, LyricLemmaStatus.VERIFIED);
            }
        }
        return new LemmaResolution(original, LyricLemmaStatus.FALLBACK);
    }

    private LyricLemmaStatus trustedOrFallback(String word) {
        return isTrustedLemma(word) ? LyricLemmaStatus.VERIFIED : LyricLemmaStatus.FALLBACK;
    }

    private boolean isTrustedLemma(String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        return validationCache.computeIfAbsent(candidate, this::lookupTrustedLemma);
    }

    private boolean lookupTrustedLemma(String candidate) {
        if (dictionaryService == null) return BUILT_IN_LEMMAS.contains(candidate);
        return dictionaryService.findWord(candidate).isPresent();
    }

    private Optional<DictionaryEntryDto> findDictionaryEntry(String word) {
        if (dictionaryService == null) return Optional.empty();
        return dictionaryService.findWord(word);
    }

    private Optional<String> morphologyLemma(String morphology, String word) {
        if (morphology == null || morphology.isBlank()) return Optional.empty();
        for (String relation : morphology.split("/")) {
            int separator = relation.indexOf(':');
            if (separator <= 0 || separator == relation.length() - 1) continue;
            if (!"0".equals(relation.substring(0, separator))) continue;
            String candidate = relation.substring(separator + 1)
                    .replaceFirst("^'+", "")
                    .toLowerCase();
            if (!candidate.isBlank() && !candidate.equals(word)) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private boolean hasDoubledFinalConsonant(String value) {
        if (value.length() < 2) return false;
        char last = value.charAt(value.length() - 1);
        char previous = value.charAt(value.length() - 2);
        return last == previous && "bcdfghjklmnpqrstvwxyz".indexOf(last) >= 0;
    }

    public record LemmaResolution(String lemma, LyricLemmaStatus status) {
    }
}
