package com.each17.backend.lyric.dto;

import java.util.List;

public record LyricDocumentDto(
        Long songId,
        String title,
        String artist,
        String album,
        String rawLyrics,
        String normalizedLyrics,
        String lyricsHash,
        Integer importVersion,
        String updatedAt,
        List<LyricLineDto> lines,
        List<com.each17.backend.dto.SongCreditDto> credits
) {
    public LyricDocumentDto(Long songId, String rawLyrics, String normalizedLyrics, String lyricsHash,
                            Integer importVersion, String updatedAt, List<LyricLineDto> lines) {
        this(songId, null, null, null, rawLyrics, normalizedLyrics, lyricsHash,
                importVersion, updatedAt, lines, List.of());
    }
}
