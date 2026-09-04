package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SongImportTaskRegistry {
    private final ConcurrentMap<UUID, ImportTaskResultDto> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> workerThreads = new ConcurrentHashMap<>();

    public void put(ImportTaskResultDto task) {
        tasks.put(task.getTaskId(), task);
    }

    public ImportTaskResultDto get(UUID taskId) {
        return tasks.get(taskId);
    }

    public void recordWorkerThread(UUID taskId, String threadName) {
        workerThreads.put(taskId, threadName);
    }

    public String workerThread(UUID taskId) {
        return workerThreads.get(taskId);
    }
}
