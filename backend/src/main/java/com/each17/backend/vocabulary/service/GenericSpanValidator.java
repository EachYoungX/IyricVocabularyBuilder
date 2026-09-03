package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class GenericSpanValidator implements SlotValidator {
    private static final Set<String> SUPPORTED = Set.of("PERSON", "THING", "OBJECT", "GENERIC");

    @Override
    public boolean supports(String slotHint) {
        return slotHint != null && SUPPORTED.contains(slotHint.toUpperCase());
    }

    @Override
    public boolean isValid(List<LyricToken> span) {
        return span != null && !span.isEmpty();
    }
}
