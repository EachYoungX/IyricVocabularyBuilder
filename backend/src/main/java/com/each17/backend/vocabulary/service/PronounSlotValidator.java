package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PronounSlotValidator implements SlotValidator {
    private static final Set<String> PRONOUNS = Set.of(
            "i", "me", "you", "he", "him", "she", "her", "it", "we", "us", "they", "them",
            "who", "whom", "whose", "which", "what", "this", "that", "these", "those",
            "someone", "somebody", "nobody", "anyone", "anybody", "everyone", "everybody", "one"
    );

    @Override
    public boolean supports(String slotHint) {
        return "PRONOUN".equalsIgnoreCase(slotHint);
    }

    @Override
    public boolean isValid(List<LyricToken> span) {
        return span != null && span.size() == 1 && PRONOUNS.contains(normalized(span.getFirst()));
    }

    private String normalized(LyricToken token) {
        String value = token == null ? "" : token.getNormalizedForm();
        return value == null ? "" : value.toLowerCase();
    }
}
