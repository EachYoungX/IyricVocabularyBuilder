package com.each17.backend.vocabulary.service;

import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.vocabulary.repository.VocabularyOverrideRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VocabularyRefreshAfterCommitTest {
    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void defersRebuildSubmissionUntilTheSourceTransactionCommits() {
        VocabularyRebuildTaskRegistry registry = mock(VocabularyRebuildTaskRegistry.class);
        VocabularyRebuildWorker worker = mock(VocabularyRebuildWorker.class);
        UUID taskId = UUID.randomUUID();
        when(registry.submit()).thenReturn(new VocabularyRebuildTaskRegistry.Submission(taskId, true));
        VocabularyServiceImpl service = service(registry, worker);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        service.requestVocabularyIndexRefreshAfterCommit();

        verifyNoInteractions(registry, worker);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.getFirst().afterCommit();

        verify(registry).submit();
        verify(worker).rebuild(taskId);
    }

    @Test
    void submitsImmediatelyWhenNoTransactionIsActive() {
        VocabularyRebuildTaskRegistry registry = mock(VocabularyRebuildTaskRegistry.class);
        VocabularyRebuildWorker worker = mock(VocabularyRebuildWorker.class);
        UUID taskId = UUID.randomUUID();
        when(registry.submit()).thenReturn(new VocabularyRebuildTaskRegistry.Submission(taskId, true));

        service(registry, worker).requestVocabularyIndexRefreshAfterCommit();

        verify(worker).rebuild(taskId);
    }

    private VocabularyServiceImpl service(
            VocabularyRebuildTaskRegistry registry,
            VocabularyRebuildWorker worker
    ) {
        return new VocabularyServiceImpl(
                mock(VocabularyRepository.class),
                mock(VocabularyOverrideRepository.class),
                mock(LyricTokenizationService.class),
                new EnglishLemmaService(),
                new ObjectMapper(),
                registry,
                worker
        );
    }
}
