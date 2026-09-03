package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ReflexiveSlotValidator implements SlotValidator {
    private static final Set<String> REFLEXIVES = Set.of(
            "myself", "yourself", "yourselves", "himself", "herself", "itself",
            "ourselves", "themselves", "oneself"
    );

    @Override
    public boolean supports(String slotHint) {
        return "REFLEXIVE".equalsIgnoreCase(slotHint);
    }

    @Override
    public boolean isValid(List<LyricToken> span) {
        return span != null && span.size() == 1 && REFLEXIVES.contains(normalized(span.getFirst()));
    }

    private String normalized(LyricToken token) {
        String value = token == null ? "" : token.getNormalizedForm();
        return value == null ? "" : value.toLowerCase();
    }
}
