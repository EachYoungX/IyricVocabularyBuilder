package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.entity.LyricToken;

import java.util.List;

public interface SlotValidator {
    boolean supports(String slotHint);

    boolean isValid(List<LyricToken> span);
}
