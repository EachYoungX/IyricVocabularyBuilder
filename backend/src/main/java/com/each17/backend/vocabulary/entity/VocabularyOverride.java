package com.each17.backend.vocabulary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vocabulary_override")
public class VocabularyOverride {
    @Id
    private String lemma;

    @Column(nullable = false)
    @Builder.Default
    private Boolean excluded = false;

    @Column(name = "recommended_override")
    private Boolean recommendedOverride;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
