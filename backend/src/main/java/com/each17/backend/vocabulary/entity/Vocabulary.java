package com.each17.backend.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vocabulary", indexes = {
        @Index(name = "idx_vocabulary_recommended_word", columnList = "recommended, word")
})
public class Vocabulary {
    @Id
    private String word;
    
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String occurrences; // 存储 JSON 字符串

    @Column(name = "display_forms", columnDefinition = "TEXT")
    private String displayForms;

    @Builder.Default
    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount = 0;

    @Builder.Default
    @Column(name = "song_count", nullable = false)
    private Integer songCount = 0;

    @Builder.Default
    @Column(name = "learning_score", nullable = false)
    private Double learningScore = 1.0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean recommended = true;
}
