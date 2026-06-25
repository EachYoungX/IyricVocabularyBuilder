package com.each17.backend.lyric.dto;

import com.each17.backend.lyric.entity.LyricLineType;

public record LyricLineUpdateRequestDto(
        String normalizedText,
        LyricLineType lineType,
        Boolean hidden
) {}
