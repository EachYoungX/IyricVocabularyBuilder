package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SongImportTaskRegistry {
    static final int MAX_RETAINED_TASKS = 256;

    private final Map<UUID, ImportTaskResultDto> tasks = new LinkedHashMap<>();
    private final Map<UUID, String> workerThreads = new LinkedHashMap<>();

    public synchronized void put(ImportTaskResultDto task) {
        tasks.put(task.getTaskId(), task);
    }

    public synchronized ImportTaskResultDto get(UUID taskId) {
        return tasks.get(taskId);
    }

    public synchronized void recordWorkerThread(UUID taskId, String threadName) {
        workerThreads.put(taskId, threadName);
    }

    public synchronized String workerThread(UUID taskId) {
        return workerThreads.get(taskId);
    }

    public synchronized void markRunning(UUID taskId) {
        ImportTaskResultDto task = tasks.get(taskId);
        if (task != null) task.setStatus("RUNNING");
    }

    public synchronized void recordSuccess(UUID taskId) {
        ImportTaskResultDto task = tasks.get(taskId);
        if (task != null) task.setSuccessCount(task.getSuccessCount() + 1);
    }

    public synchronized void recordFailure(UUID taskId, ImportTaskResultDto.FailedItemDto failedItem) {
        ImportTaskResultDto task = tasks.get(taskId);
        if (task == null) return;
        task.setFailedCount(task.getFailedCount() + 1);
        task.getFailedItems().add(failedItem);
    }

    public synchronized void markCompleted(UUID taskId) {
        ImportTaskResultDto task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setFinishedAt(LocalDateTime.now());
        }
        pruneCompletedHistory(taskId);
    }

    private void pruneCompletedHistory(UUID preservedTaskId) {
        if (tasks.size() <= MAX_RETAINED_TASKS) return;
        Iterator<Map.Entry<UUID, ImportTaskResultDto>> iterator = tasks.entrySet().iterator();
        while (tasks.size() > MAX_RETAINED_TASKS && iterator.hasNext()) {
            Map.Entry<UUID, ImportTaskResultDto> entry = iterator.next();
            if (!entry.getKey().equals(preservedTaskId) && "COMPLETED".equals(entry.getValue().getStatus())) {
                workerThreads.remove(entry.getKey());
                iterator.remove();
            }
        }
    }
}
