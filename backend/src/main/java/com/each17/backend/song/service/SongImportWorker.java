package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        task.setStatus("RUNNING");
        log.info("[Task {}] Import worker started on thread {}", taskId, Thread.currentThread().getName());

        for (int index = 0; index < songs.size(); index++) {
            SongImportRequestDto song = songs.get(index);
            try {
                transaction.importOne(song, autoAddToPersonalVocabulary);
                task.setSuccessCount(task.getSuccessCount() + 1);
            } catch (DataIntegrityViolationException exception) {
                recordFailure(task, index, song, "Song already exists with different lyrics");
            } catch (Exception exception) {
                log.warn("[Task {}] Song import failed at index {}", taskId, index, exception);
                recordFailure(task, index, song, exception.getMessage());
            }
        }

        task.setStatus("COMPLETED");
        task.setFinishedAt(LocalDateTime.now());
        if (task.getSuccessCount() > 0) vocabularyService.refreshVocabularyIndexAsync();
        log.info("[Task {}] Import finished: {} succeeded, {} failed",
                taskId, task.getSuccessCount(), task.getFailedCount());
    }

    private void recordFailure(ImportTaskResultDto task, int index, SongImportRequestDto song, String error) {
        task.setFailedCount(task.getFailedCount() + 1);
        task.getFailedItems().add(ImportTaskResultDto.FailedItemDto.builder()
                .index(index)
                .title(song == null ? null : song.getTitle())
                .artist(song == null ? null : song.getArtist())
                .error(error == null || error.isBlank() ? "Import failed" : error)
                .build());
    }
}
