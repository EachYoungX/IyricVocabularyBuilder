package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.VocabularyRebuildTaskDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class VocabularyRebuildTaskRegistry {
    static final int MAX_RETAINED_TASKS = 256;

    private final Map<UUID, VocabularyRebuildTaskDto> tasks = new LinkedHashMap<>();
    private final Map<UUID, String> workerThreads = new LinkedHashMap<>();
    private UUID activeTaskId;
    private boolean rerunRequested;

    public synchronized Submission submit() {
        if (activeTaskId != null) {
            VocabularyRebuildTaskDto active = tasks.get(activeTaskId);
            if (active != null && ("PENDING".equals(active.getStatus()) || "RUNNING".equals(active.getStatus()))) {
                rerunRequested = true;
                return new Submission(activeTaskId, false);
            }
        }
        UUID taskId = UUID.randomUUID();
        tasks.put(taskId, VocabularyRebuildTaskDto.builder()
                .taskId(taskId)
                .status("PENDING")
                .startedAt(LocalDateTime.now())
                .build());
        activeTaskId = taskId;
        rerunRequested = false;
        return new Submission(taskId, true);
    }

    public synchronized VocabularyRebuildTaskDto get(UUID taskId) {
        return tasks.get(taskId);
    }

    public synchronized void markRunning(UUID taskId) {
        VocabularyRebuildTaskDto task = tasks.get(taskId);
        if (task != null) task.setStatus("RUNNING");
    }

    public synchronized void recordWorkerThread(UUID taskId, String threadName) {
        workerThreads.put(taskId, threadName);
    }

    public synchronized String workerThread(UUID taskId) {
        return workerThreads.get(taskId);
    }

    public synchronized boolean completeOrContinue(UUID taskId) {
        if (!taskId.equals(activeTaskId)) return false;
        if (rerunRequested) {
            rerunRequested = false;
            return true;
        }
        finish(taskId, "COMPLETED", null);
        return false;
    }

    public synchronized void markFailed(UUID taskId, String message) {
        finish(taskId, "FAILED", message);
    }

    private void finish(UUID taskId, String status, String message) {
        VocabularyRebuildTaskDto task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(message);
            task.setFinishedAt(LocalDateTime.now());
        }
        if (taskId.equals(activeTaskId)) {
            activeTaskId = null;
            rerunRequested = false;
        }
        pruneCompletedHistory();
    }

    private void pruneCompletedHistory() {
        if (tasks.size() <= MAX_RETAINED_TASKS) return;
        Iterator<Map.Entry<UUID, VocabularyRebuildTaskDto>> iterator = tasks.entrySet().iterator();
        while (tasks.size() > MAX_RETAINED_TASKS && iterator.hasNext()) {
            Map.Entry<UUID, VocabularyRebuildTaskDto> entry = iterator.next();
            String status = entry.getValue().getStatus();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                workerThreads.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    public record Submission(UUID taskId, boolean newlyCreated) {
    }
}
