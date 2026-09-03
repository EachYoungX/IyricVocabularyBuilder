package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PossessiveSlotValidator implements SlotValidator {
    private static final Set<String> DETERMINERS = Set.of("my", "your", "his", "her", "its", "our", "their", "one's");

    @Override
    public boolean supports(String slotHint) {
        return "POSSESSIVE".equalsIgnoreCase(slotHint);
    }

    @Override
    public boolean isValid(List<LyricToken> span) {
        if (span == null || span.isEmpty()) return false;
        return DETERMINERS.contains(normalized(span.getFirst())) || hasPossessiveMarker(span.getLast());
    }

    private boolean hasPossessiveMarker(LyricToken token) {
        String value = normalized(token);
        return value.endsWith("'s") || value.endsWith("s'");
    }

    private String normalized(LyricToken token) {
        String value = token == null ? "" : token.getNormalizedForm();
        return value == null ? "" : value.toLowerCase().replace('’', '\'');
    }
}
