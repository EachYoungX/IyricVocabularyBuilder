package com.each17.backend.vocabulary.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VocabularyRebuildTaskRegistryTest {
    @Test
    void activeSubmissionRequestsAnotherRoundWithoutCreatingAConcurrentTask() {
        VocabularyRebuildTaskRegistry registry = new VocabularyRebuildTaskRegistry();
        VocabularyRebuildTaskRegistry.Submission first = registry.submit();
        registry.markRunning(first.taskId());

        VocabularyRebuildTaskRegistry.Submission second = registry.submit();

        assertEquals(first.taskId(), second.taskId());
        assertFalse(second.newlyCreated());
        assertTrue(registry.completeOrContinue(first.taskId()));
        assertEquals("RUNNING", registry.get(first.taskId()).getStatus());
        assertFalse(registry.completeOrContinue(first.taskId()));
        assertEquals("COMPLETED", registry.get(first.taskId()).getStatus());
        assertNotEquals(first.taskId(), registry.submit().taskId());
    }

    @Test
    void retainsOnlyBoundedCompletedHistory() {
        VocabularyRebuildTaskRegistry registry = new VocabularyRebuildTaskRegistry();
        UUID firstTaskId = null;
        UUID lastTaskId = null;
        for (int index = 0; index <= VocabularyRebuildTaskRegistry.MAX_RETAINED_TASKS; index += 1) {
            UUID taskId = registry.submit().taskId();
            if (firstTaskId == null) firstTaskId = taskId;
            lastTaskId = taskId;
            registry.markRunning(taskId);
            registry.completeOrContinue(taskId);
        }

        assertNull(registry.get(firstTaskId));
        assertEquals("COMPLETED", registry.get(lastTaskId).getStatus());
    }
}
