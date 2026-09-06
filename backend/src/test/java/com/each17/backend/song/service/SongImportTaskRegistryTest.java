package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SongImportTaskRegistryTest {
    @Test
    void retainsOnlyBoundedCompletedHistory() {
        SongImportTaskRegistry registry = new SongImportTaskRegistry();
        UUID firstTaskId = null;
        UUID lastTaskId = null;
        for (int index = 0; index <= SongImportTaskRegistry.MAX_RETAINED_TASKS; index += 1) {
            UUID taskId = UUID.randomUUID();
            if (firstTaskId == null) firstTaskId = taskId;
            lastTaskId = taskId;
            registry.put(ImportTaskResultDto.builder()
                    .taskId(taskId)
                    .status("PENDING")
                    .total(0)
                    .successCount(0)
                    .failedCount(0)
                    .failedItems(new ArrayList<>())
                    .build());
            registry.markCompleted(taskId);
        }

        assertNull(registry.get(firstTaskId));
        assertEquals("COMPLETED", registry.get(lastTaskId).getStatus());
    }
}
