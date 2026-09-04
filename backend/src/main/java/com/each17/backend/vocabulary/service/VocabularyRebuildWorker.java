package com.each17.backend.vocabulary.service;

import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.entity.Vocabulary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyRebuildWorker {
    private final VocabularyRebuildTaskRegistry registry;
    private final SongRepository songRepository;
    private final VocabularyIndexBuilder indexBuilder;
    private final VocabularyRebuildTransaction rebuildTransaction;

    @Async
    public void rebuild(UUID taskId) {
        registry.recordWorkerThread(taskId, Thread.currentThread().getName());
        registry.markRunning(taskId);
        log.info("[Refresh Task {}] Rebuilding vocabulary index", taskId);
        try {
            List<Song> songs = songRepository.findAll();
            List<Vocabulary> generated = indexBuilder.rebuildFromSongs(songs);
            int saved = rebuildTransaction.replace(generated);
            registry.markCompleted(taskId);
            log.info("[Refresh Task {}] Vocabulary rebuilt successfully: {} words", taskId, saved);
        } catch (Exception exception) {
            registry.markFailed(taskId, exception.getMessage());
            log.error("[Refresh Task {}] Rebuild failed", taskId, exception);
        }
    }
}
