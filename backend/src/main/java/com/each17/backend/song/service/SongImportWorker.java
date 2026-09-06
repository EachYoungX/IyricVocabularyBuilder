package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongImportWorker {
    private final SongImportTaskRegistry registry;
    private final SongImportTransaction transaction;
    private final VocabularyService vocabularyService;

    @Async
    public void process(UUID taskId, List<SongImportRequestDto> songs, boolean autoAddToPersonalVocabulary) {
        ImportTaskResultDto task = registry.get(taskId);
        if (task == null) {
            log.error("[Task {}] Import task was not registered", taskId);
            return;
        }
        registry.recordWorkerThread(taskId, Thread.currentThread().getName());
        registry.markRunning(taskId);
        log.info("[Task {}] Import worker started on thread {}", taskId, Thread.currentThread().getName());

        for (int index = 0; index < songs.size(); index++) {
            SongImportRequestDto song = songs.get(index);
            try {
                transaction.importOne(song, autoAddToPersonalVocabulary);
                registry.recordSuccess(taskId);
            } catch (DataIntegrityViolationException exception) {
                recordFailure(taskId, index, song, "Song already exists with different lyrics");
            } catch (Exception exception) {
                log.warn("[Task {}] Song import failed at index {}", taskId, index, exception);
                recordFailure(taskId, index, song, exception.getMessage());
            }
        }

        registry.markCompleted(taskId);
        task = registry.get(taskId);
        if (task == null) return;
        if (task.getSuccessCount() > 0) vocabularyService.refreshVocabularyIndexAsync();
        log.info("[Task {}] Import finished: {} succeeded, {} failed",
                taskId, task.getSuccessCount(), task.getFailedCount());
    }

    private void recordFailure(UUID taskId, int index, SongImportRequestDto song, String error) {
        registry.recordFailure(taskId, ImportTaskResultDto.FailedItemDto.builder()
                .index(index)
                .title(song == null ? null : song.getTitle())
                .artist(song == null ? null : song.getArtist())
                .error(error == null || error.isBlank() ? "Import failed" : error)
                .build());
    }
}
