package com.each17.backend.service;

import com.each17.backend.dto.ImportTaskResultDto;

import java.util.UUID;

public interface ImportTaskService {
    void startImportTask(UUID taskId, Object taskData);
    ImportTaskResultDto getTaskResult(UUID taskId);
    void updateTaskStatus(UUID taskId, String status);
}