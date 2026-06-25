package com.each17.backend.song.entity;

import jakarta.persistence.*;
import lombok.*;

@Data // Lombok 注解，自动生成 Getter, Setter, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "songs", uniqueConstraints = @UniqueConstraint(columnNames = {"title", "artist"}))
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artist;
    
    @Lob // 表示这是一个大的文本字段
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lyrics;

    @Lob
    @Column(name = "raw_lyrics", columnDefinition = "TEXT")
    private String rawLyrics;

    @Lob
    @Column(name = "normalized_lyrics", columnDefinition = "TEXT")
    private String normalizedLyrics;

    @Column(name = "lyrics_hash")
    private String lyricsHash;

    @Column(name = "import_version", nullable = false)
    @Builder.Default
    private Integer importVersion = 1;

    @Column(name = "updated_at")
    private String updatedAt;
}
