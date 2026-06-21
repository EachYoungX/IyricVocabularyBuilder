package com.each17.backend.service.impl;

import com.each17.backend.dto.ImportTaskResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImportTaskServiceImplTest {

    private ImportTaskServiceImpl importTaskService;

    @BeforeEach
    void setUp() {
        importTaskService = new ImportTaskServiceImpl();
    }

    @Test
    void testStartImportTask() {
        // Given
        UUID taskId = UUID.randomUUID();
        Object taskData = new Object();

        // When
        importTaskService.startImportTask(taskId, taskData);

        // Then
        ImportTaskResultDto result = importTaskService.getTaskResult(taskId);
        assertEquals(taskId, result.getTaskId());
        assertEquals("RUNNING", result.getStatus());
    }

    @Test
    void testGetTaskResult() {
        // Given
        UUID taskId = UUID.randomUUID();

        // When
        ImportTaskResultDto result = importTaskService.getTaskResult(taskId);

        // Then
        assertEquals(taskId, result.getTaskId());
        assertEquals("NOT_FOUND", result.getStatus());
    }

    @Test
    void testUpdateTaskStatus() {
        // Given
        UUID taskId = UUID.randomUUID();
        Object taskData = new Object();
        importTaskService.startImportTask(taskId, taskData);

        // When
        importTaskService.updateTaskStatus(taskId, "COMPLETED");

        // Then
        ImportTaskResultDto result = importTaskService.getTaskResult(taskId);
        assertEquals(taskId, result.getTaskId());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void testUpdateTaskStatusNotFound() {
        // Given
        UUID taskId = UUID.randomUUID();

        // When
        importTaskService.updateTaskStatus(taskId, "COMPLETED");

        // Then
        ImportTaskResultDto result = importTaskService.getTaskResult(taskId);
        assertEquals(taskId, result.getTaskId());
        assertEquals("NOT_FOUND", result.getStatus());
    }
}