package com.each17.backend.lyric.entity;

import com.each17.backend.song.entity.Song;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lyric_lines", uniqueConstraints = @UniqueConstraint(columnNames = {"song_id", "line_index"}))
public class LyricLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "line_index", nullable = false)
    private Integer lineIndex;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "normalized_text", nullable = false, columnDefinition = "TEXT")
    private String normalizedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false)
    private LyricLineType lineType;

    @Column(nullable = false)
    private Boolean hidden;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "user_override", nullable = false)
    private Boolean userOverride;
}
