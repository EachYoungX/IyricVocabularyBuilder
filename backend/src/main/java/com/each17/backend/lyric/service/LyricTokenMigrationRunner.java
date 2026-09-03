package com.each17.backend.lyric.service;

import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.VocabularyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class LyricTokenMigrationRunner implements ApplicationRunner {
    private static final String VERSION = "lyric-structure-v5";
    private static final String LINE_CLASSIFIER_VERSION = "5";
    private static final String PHRASE_MATCHER_VERSION = "4";

    @Qualifier("appJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;
    private final SongRepository songRepository;
    private final LyricStructureService lyricStructureService;
    private final VocabularyService vocabularyService;

    public LyricTokenMigrationRunner(
            @Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
            SongRepository songRepository,
            LyricStructureService lyricStructureService,
            VocabularyService vocabularyService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.songRepository = songRepository;
        this.lyricStructureService = lyricStructureService;
        this.vocabularyService = vocabularyService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String current = jdbcTemplate.query("SELECT value FROM app_meta WHERE key = 'lyric.token.migration'",
                rows -> rows.next() ? rows.getString(1) : null);
        if (VERSION.equals(current)) {
            writeVersionMetadata();
            return;
        }
        for (Song song : songRepository.findAll()) lyricStructureService.reclassifySong(song.getId());
        jdbcTemplate.update("INSERT OR REPLACE INTO app_meta(key, value) VALUES ('lyric.token.migration', ?)", VERSION);
        writeVersionMetadata();
        if (!songRepository.findAll().isEmpty()) vocabularyService.refreshVocabularyIndexAsync();
    }

    private void writeVersionMetadata() {
        jdbcTemplate.update("INSERT OR REPLACE INTO app_meta(key, value) VALUES ('line.classifier.version', ?)",
                LINE_CLASSIFIER_VERSION);
        jdbcTemplate.update("INSERT OR REPLACE INTO app_meta(key, value) VALUES ('phrase.matcher.version', ?)",
                PHRASE_MATCHER_VERSION);
    }
}
