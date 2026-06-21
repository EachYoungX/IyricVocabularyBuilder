// com.each17.backend.dto.VocabularyRebuildTaskDto
package com.each17.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyRebuildTaskDto {
    private UUID taskId;
    private String status;        // PENDING / RUNNING / COMPLETED / FAILED
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;  // 只有失败时才有值
}