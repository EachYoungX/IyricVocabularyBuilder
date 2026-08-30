package com.each17.backend.lyric.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lyric_tokens", indexes = {
        @Index(name = "idx_lyric_tokens_line", columnList = "lyric_line_id"),
        @Index(name = "idx_lyric_tokens_line_position", columnList = "lyric_line_id, token_position", unique = true),
        @Index(name = "idx_lyric_tokens_lemma", columnList = "lemma"),
        @Index(name = "idx_lyric_tokens_normalized", columnList = "normalized_form"),
        @Index(name = "idx_lyric_tokens_lemma_location", columnList = "lemma, lyric_line_id, token_position")
})
public class LyricToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lyric_line_id", nullable = false)
    private LyricLine lyricLine;

    @Column(name = "token_position", nullable = false)
    private Integer tokenPosition;

    @Column(name = "surface_form", nullable = false)
    private String surfaceForm;

    @Column(name = "normalized_form", nullable = false)
    private String normalizedForm;

    @Column(nullable = false)
    private String lemma;

    @Enumerated(EnumType.STRING)
    @Column(name = "lemma_status", nullable = false)
    private LyricLemmaStatus lemmaStatus;

    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private LyricTokenType tokenType;

    @Column(name = "learning_score", nullable = false)
    private Double learningScore;
}
