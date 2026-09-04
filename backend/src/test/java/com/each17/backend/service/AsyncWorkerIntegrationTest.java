package com.each17.backend.service;

import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.song.service.SongImportTaskRegistry;
import com.each17.backend.song.service.SongImportWorker;
import com.each17.backend.song.service.SongService;
import com.each17.backend.vocabulary.service.VocabularyRebuildTaskRegistry;
import com.each17.backend.vocabulary.service.VocabularyRebuildWorker;
import com.each17.backend.vocabulary.service.VocabularyService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AsyncWorkerIntegrationTest {
    @Autowired
    private SongService songService;
    @Autowired
    private SongImportWorker songImportWorker;
    @Autowired
    private SongImportTaskRegistry songRegistry;
    @Autowired
    private VocabularyService vocabularyService;
    @Autowired
    private VocabularyRebuildWorker vocabularyRebuildWorker;
    @Autowired
    private VocabularyRebuildTaskRegistry vocabularyRegistry;

    @Test
    void songImportRunsThroughAsyncProxyOnAnotherThread() throws Exception {
        assertTrue(AopUtils.isAopProxy(songImportWorker));
        String callerThread = Thread.currentThread().getName();
        SongImportRequestDto invalid = new SongImportRequestDto();
        UUID taskId = songService.importSongsAsync(List.of(invalid)).getTaskId();

        await(() -> songRegistry.workerThread(taskId) != null
                && "COMPLETED".equals(songRegistry.get(taskId).getStatus()));
        assertNotEquals(callerThread, songRegistry.workerThread(taskId));
    }

    @Test
    void vocabularyRebuildRunsThroughAsyncProxyOnAnotherThread() throws Exception {
        assertTrue(AopUtils.isAopProxy(vocabularyRebuildWorker));
        String callerThread = Thread.currentThread().getName();
        UUID taskId = vocabularyService.refreshVocabularyIndexAsync();

        await(() -> vocabularyRegistry.workerThread(taskId) != null
                && isFinished(vocabularyRegistry.get(taskId).getStatus()));
        assertNotEquals(callerThread, vocabularyRegistry.workerThread(taskId));
    }

    private boolean isFinished(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

    private void await(CheckedBoolean condition) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!condition.get() && Instant.now().isBefore(deadline)) Thread.sleep(20);
        assertTrue(condition.get(), "Async task did not finish before timeout");
    }

    @FunctionalInterface
    private interface CheckedBoolean {
        boolean get() throws Exception;
    }
}
