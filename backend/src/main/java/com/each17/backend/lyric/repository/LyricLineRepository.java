package com.each17.backend.lyric.repository;

import com.each17.backend.lyric.entity.LyricLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LyricLineRepository extends JpaRepository<LyricLine, Long> {
    List<LyricLine> findBySongIdOrderByLineIndexAsc(Long songId);

    @Query("""
            select line from LyricLine line
            join fetch line.song song
            where song.id in :songIds
            order by song.id asc, line.lineIndex asc
            """)
    List<LyricLine> findBySongIdsOrderBySongAndLineIndex(Collection<Long> songIds);

    Optional<LyricLine> findByIdAndSongId(Long id, Long songId);
    boolean existsBySongId(Long songId);
    void deleteBySongId(Long songId);
}
