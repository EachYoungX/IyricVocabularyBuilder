package com.each17.backend.lyric.dto;

import java.util.List;

public record LyricDocumentDto(
        Long songId,
        String rawLyrics,
        String normalizedLyrics,
        String lyricsHash,
        Integer importVersion,
        String updatedAt,
        List<LyricLineDto> lines
) {}
