package com.each17.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTaskResultDto {
    private UUID taskId;
    private String status;
    private Integer total;
    private Integer successCount;
    private Integer failedCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<FailedItemDto> failedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItemDto {
        private Integer index;
        private String title;
        private String artist;
        private String lyricsSnippet;
        private String error;
    }
}