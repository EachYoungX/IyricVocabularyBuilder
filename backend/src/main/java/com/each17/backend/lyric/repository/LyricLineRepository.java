package com.each17.backend.lyric.repository;

import com.each17.backend.lyric.entity.LyricLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LyricLineRepository extends JpaRepository<LyricLine, Long> {
    List<LyricLine> findBySongIdOrderByLineIndexAsc(Long songId);
    Optional<LyricLine> findByIdAndSongId(Long id, Long songId);
    boolean existsBySongId(Long songId);
    void deleteBySongId(Long songId);
}
