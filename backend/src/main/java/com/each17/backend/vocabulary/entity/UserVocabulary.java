package com.each17.backend.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_vocabulary",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lemma"}),
        indexes = {
                @Index(name = "idx_user_vocabulary_user_status", columnList = "user_id, status"),
                @Index(name = "idx_user_vocabulary_user_lemma", columnList = "user_id, lemma")
        }
)
public class UserVocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String lemma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VocabularyStatus status;

    @Column(name = "mastery_score", nullable = false)
    private Double masteryScore;

    @Column(name = "first_seen_at", nullable = false)
    private String firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private String lastSeenAt;

    @Column(name = "review_due_at")
    private String reviewDueAt;

    @Column(columnDefinition = "TEXT")
    private String note;
}
