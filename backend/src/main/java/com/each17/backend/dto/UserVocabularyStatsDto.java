package com.each17.backend.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyStatsDto {
    private Long totalCount;
    private Long newCount;
    private Long learningCount;
    private Long familiarCount;
    private Long masteredCount;
    private Long ignoredCount;
    private Long dueReviewCount;
    private List<UserVocabularyDto> recentWords;
}
