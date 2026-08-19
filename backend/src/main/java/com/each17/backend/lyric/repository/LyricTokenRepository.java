package com.each17.backend.lyric.repository;

import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LyricTokenRepository extends JpaRepository<LyricToken, Long> {
    List<LyricToken> findByLemma(String lemma);

    List<LyricToken> findDistinctByLyricLineSongIdAndLearningScoreGreaterThan(Long songId, Double score);

    @Modifying
    @Query("delete from LyricToken token where token.lyricLine.song.id = :songId")
    void deleteBySongId(Long songId);
}
