package com.each17.backend.song.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "song_credit")
public class SongCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_type", nullable = false)
    private SongCreditType creditType;

    @Column(name = "credit_label")
    private String creditLabel;

    @Column(name = "credit_value", nullable = false, columnDefinition = "TEXT")
    private String creditValue;

    @Column(name = "source_line_id")
    private Long sourceLineId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
