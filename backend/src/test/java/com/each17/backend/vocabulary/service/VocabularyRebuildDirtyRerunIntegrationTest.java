package com.each17.backend.vocabulary.service;

import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
class VocabularyRebuildDirtyRerunIntegrationTest {
    @Autowired
    private VocabularyService vocabularyService;
    @Autowired
    private VocabularyRebuildTaskRegistry registry;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean
    private VocabularyIndexBuilder indexBuilder;

    @BeforeEach
    void setUp() {
        Mockito.reset(indexBuilder);
        clearData();
    }

    @AfterEach
    void tearDown() {
        clearData();
        Mockito.reset(indexBuilder);
    }

    @Test
    void requestDuringActiveRebuildRunsAnotherRoundWithLatestSongData() throws Exception {
        Song song = songRepository.saveAndFlush(Song.builder()
                .title("Rebuild Race")
                .artist("Integration Test")
                .lyrics("alpha")
                .rawLyrics("alpha")
                .normalizedLyrics("alpha")
                .importVersion(1)
                .build());

        CountDownLatch firstRoundGenerated = new CountDownLatch(1);
        CountDownLatch releaseFirstRound = new CountDownLatch(1);
        AtomicInteger rounds = new AtomicInteger();
        doAnswer(invocation -> {
            Object generated = invocation.callRealMethod();
            if (rounds.incrementAndGet() == 1) {
                firstRoundGenerated.countDown();
                if (!releaseFirstRound.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release the first rebuild round");
                }
            }
            return generated;
        }).when(indexBuilder).rebuildFromSongs(anyList());

        UUID firstTaskId = vocabularyService.refreshVocabularyIndexAsync();
        assertTrue(firstRoundGenerated.await(5, TimeUnit.SECONDS), "First rebuild did not reach the pause point");

        song.setLyrics("beta");
        song.setRawLyrics("beta");
        song.setNormalizedLyrics("beta");
        songRepository.saveAndFlush(song);
        UUID secondTaskId = vocabularyService.refreshVocabularyIndexAsync();
        assertEquals(firstTaskId, secondTaskId, "Concurrent requests should share the active task");

        releaseFirstRound.countDown();
        await(() -> rounds.get() >= 2
                && "COMPLETED".equals(registry.get(firstTaskId).getStatus()));

        assertEquals(2, rounds.get());
        assertEquals(List.of("beta"), jdbcTemplate.queryForList(
                "SELECT word FROM vocabulary ORDER BY word", String.class));
    }

    private void clearData() {
        jdbcTemplate.update("DELETE FROM phrase_occurrence");
        jdbcTemplate.update("DELETE FROM phrase_cache_state");
        jdbcTemplate.update("DELETE FROM vocabulary_occurrences");
        jdbcTemplate.update("DELETE FROM song_credit");
        jdbcTemplate.update("DELETE FROM lyric_tokens");
        jdbcTemplate.update("DELETE FROM lyric_lines");
        jdbcTemplate.update("DELETE FROM songs");
        jdbcTemplate.update("DELETE FROM vocabulary_override");
        jdbcTemplate.update("DELETE FROM vocabulary");
    }

    private void await(CheckedBoolean condition) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
        while (!condition.get() && Instant.now().isBefore(deadline)) Thread.sleep(20);
        assertTrue(condition.get(), "Rebuild task did not finish before timeout");
    }

    @FunctionalInterface
    private interface CheckedBoolean {
        boolean get() throws Exception;
    }
}
