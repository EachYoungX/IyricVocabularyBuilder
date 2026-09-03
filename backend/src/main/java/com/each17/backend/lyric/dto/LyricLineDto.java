package com.each17.backend.lyric.dto;

import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricClassificationSource;

public record LyricLineDto(
        Long id,
        Integer lineIndex,
        String originalText,
        String normalizedText,
        LyricLineType lineType,
        LyricClassificationSource classificationSource,
        Boolean hidden,
        Double confidence,
        Boolean userOverride
) {
    public LyricLineDto(Long id, Integer lineIndex, String originalText, String normalizedText,
                        LyricLineType lineType, Boolean hidden, Double confidence, Boolean userOverride) {
        this(id, lineIndex, originalText, normalizedText, lineType,
                LyricClassificationSource.DEFAULT, hidden, confidence, userOverride);
    }
}
