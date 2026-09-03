package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricLemmaStatus;
import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GerundSlotValidator implements SlotValidator {
    @Override
    public boolean supports(String slotHint) {
        return "GERUND".equalsIgnoreCase(slotHint);
    }

    @Override
    public boolean isValid(List<LyricToken> span) {
        if (span == null || span.isEmpty()) return false;
        LyricToken core = span.getFirst();
        String normalized = core.getNormalizedForm();
        return normalized != null
                && normalized.toLowerCase().endsWith("ing")
                && core.getLemmaStatus() == LyricLemmaStatus.VERIFIED;
    }
}
