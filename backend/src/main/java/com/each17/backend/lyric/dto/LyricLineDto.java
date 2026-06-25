package com.each17.backend.lyric.dto;

import com.each17.backend.lyric.entity.LyricLineType;

public record LyricLineDto(
        Long id,
        Integer lineIndex,
        String originalText,
        String normalizedText,
        LyricLineType lineType,
        Boolean hidden,
        Double confidence,
        Boolean userOverride
) {}
