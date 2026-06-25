package com.each17.backend.lyric.service;

import org.springframework.stereotype.Service;

import java.util.Map;

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

    public String lemma(String normalizedForm) {
        if (normalizedForm == null || normalizedForm.isBlank()) return "";
        String word = normalizedForm.toLowerCase();
        if (IRREGULARS.containsKey(word)) return IRREGULARS.get(word);
        if (word.length() <= 3) return word;

        if (word.endsWith("'s") || word.endsWith("s'")) {
            word = word.substring(0, word.length() - 2);
        }

        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("ied") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("ing") && word.length() > 5) {
            String stem = word.substring(0, word.length() - 3);
            if (hasDoubledFinalConsonant(stem)) stem = stem.substring(0, stem.length() - 1);
            if (stem.length() > 2 && !stem.endsWith("e")) {
                return stem;
            }
        }
        if (word.endsWith("ed") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 2);
            if (hasDoubledFinalConsonant(stem)) stem = stem.substring(0, stem.length() - 1);
            if (stem.endsWith("i")) return stem.substring(0, stem.length() - 1) + "y";
            return stem;
        }
        if (word.endsWith("es") && word.length() > 4 && (word.endsWith("ses") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("ches") || word.endsWith("shes"))) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 3) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private boolean hasDoubledFinalConsonant(String value) {
        if (value.length() < 2) return false;
        char last = value.charAt(value.length() - 1);
        char previous = value.charAt(value.length() - 2);
        return last == previous && "bcdfghjklmnpqrstvwxyz".indexOf(last) >= 0;
    }
}
