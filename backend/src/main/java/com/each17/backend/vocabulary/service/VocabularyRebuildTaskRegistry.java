package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.VocabularyRebuildTaskDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class VocabularyRebuildTaskRegistry {
    private final ConcurrentMap<UUID, VocabularyRebuildTaskDto> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> workerThreads = new ConcurrentHashMap<>();
    private UUID activeTaskId;

    public synchronized Submission submit() {
        if (activeTaskId != null) {
            VocabularyRebuildTaskDto active = tasks.get(activeTaskId);
            if (active != null && ("PENDING".equals(active.getStatus()) || "RUNNING".equals(active.getStatus()))) {
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
        return new Submission(taskId, true);
    }

    public VocabularyRebuildTaskDto get(UUID taskId) {
        return tasks.get(taskId);
    }

    public void markRunning(UUID taskId) {
        VocabularyRebuildTaskDto task = tasks.get(taskId);
        if (task != null) task.setStatus("RUNNING");
    }

    public void recordWorkerThread(UUID taskId, String threadName) {
        workerThreads.put(taskId, threadName);
    }

    public String workerThread(UUID taskId) {
        return workerThreads.get(taskId);
    }

    public void markCompleted(UUID taskId) {
        finish(taskId, "COMPLETED", null);
    }

    public void markFailed(UUID taskId, String message) {
        finish(taskId, "FAILED", message);
    }

    private synchronized void finish(UUID taskId, String status, String message) {
        VocabularyRebuildTaskDto task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(message);
            task.setFinishedAt(LocalDateTime.now());
        }
        if (taskId.equals(activeTaskId)) activeTaskId = null;
    }

    public record Submission(UUID taskId, boolean newlyCreated) {
    }
}
