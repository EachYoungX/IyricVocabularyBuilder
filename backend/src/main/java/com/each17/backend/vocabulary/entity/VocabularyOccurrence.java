package com.each17.backend.vocabulary.entity;

import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.song.entity.Song;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "vocabulary_occurrences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_vocabulary_id", "token_id"}),
        indexes = {
                @Index(name = "idx_vocabulary_occurrences_song", columnList = "song_id"),
                @Index(name = "idx_vocabulary_occurrences_user_vocab", columnList = "user_vocabulary_id")
        }
)
public class VocabularyOccurrence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_vocabulary_id")
    private UserVocabulary userVocabulary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lyric_line_id", nullable = false)
    private LyricLine lyricLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false)
    private LyricToken token;
}
