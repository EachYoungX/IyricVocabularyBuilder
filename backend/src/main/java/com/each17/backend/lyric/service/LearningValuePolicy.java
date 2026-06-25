package com.each17.backend.lyric.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LearningValuePolicy {
    private static final Set<String> FUNCTION_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "do", "for", "from", "have",
            "he", "her", "his", "i", "if", "in", "is", "it", "me", "my", "not", "of", "on", "or", "our",
            "she", "so", "that", "the", "their", "them", "they", "this", "to", "was", "we", "were", "with",
            "you", "your"
    );

    private static final Set<String> LYRIC_FILLERS = Set.of(
            "ah", "ahh", "ahhh", "la", "na", "o", "oh", "ooh", "oooh", "uh", "uhh", "um", "mmm", "woo",
            "woah", "yeah", "ya", "yall", "em", "cause", "til"
    );

    public double score(String normalizedForm, String lemma) {
        if (normalizedForm == null || normalizedForm.isBlank() || lemma == null || lemma.isBlank()) return 0.0;
        if (normalizedForm.chars().noneMatch(Character::isLetter)) return 0.0;
        if (normalizedForm.matches("(.)\\1{3,}")) return 0.15;
        if (LYRIC_FILLERS.contains(normalizedForm) || LYRIC_FILLERS.contains(lemma)) return 0.25;
        if (FUNCTION_WORDS.contains(lemma)) return 0.35;
        if (lemma.length() <= 1) return 0.2;
        return 1.0;
    }

    public boolean recommended(double score) {
        return score >= 0.5;
    }
}
