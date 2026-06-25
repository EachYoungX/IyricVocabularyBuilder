package com.each17.backend.lyric.service;

import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LyricBackfillRunner implements ApplicationRunner {
    private final SongRepository songRepository;
    private final LyricLineRepository lyricLineRepository;
    private final LyricStructureService lyricStructureService;

    @Override
    public void run(ApplicationArguments args) {
        songRepository.findAll().stream()
                .filter(song -> !lyricLineRepository.existsBySongId(song.getId()))
                .forEach(song -> lyricStructureService.structureSong(
                        song,
                        song.getRawLyrics() != null ? song.getRawLyrics() : song.getLyrics(),
                        true
                ));
    }
}
