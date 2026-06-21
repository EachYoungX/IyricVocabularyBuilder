package com.each17.backend.service.impl;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.service.ImportTaskService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportTaskServiceImpl implements ImportTaskService {
    
    private final Map<UUID, ImportTaskResultDto> taskResults = new ConcurrentHashMap<>();

    @Override
    public void startImportTask(UUID taskId, Object taskData) {
        // 启动导入任务
        ImportTaskResultDto result = ImportTaskResultDto.builder()
                .taskId(taskId)
                .status("RUNNING")
                .build();
        taskResults.put(taskId, result);
    }

    @Override
    public ImportTaskResultDto getTaskResult(UUID taskId) {
        return taskResults.getOrDefault(taskId, ImportTaskResultDto.builder()
                .taskId(taskId)
                .status("NOT_FOUND")
                .build());
    }

    @Override
    public void updateTaskStatus(UUID taskId, String status) {
        ImportTaskResultDto result = taskResults.get(taskId);
        if (result != null) {
            result.setStatus(status);
            taskResults.put(taskId, result);
        }
    }
}