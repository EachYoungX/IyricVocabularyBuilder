package com.each17.backend.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_phrase", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "canonical_phrase"}))
public class UserPhrase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "user_id", nullable = false)
    private String userId = "local";

    @Column(name = "canonical_phrase", nullable = false)
    private String canonicalPhrase;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(name = "created_at", nullable = false)
    private String createdAt;
}
