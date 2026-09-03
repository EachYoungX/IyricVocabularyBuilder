package com.each17.backend.song.repository;

import com.each17.backend.song.entity.SongCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SongCreditRepository extends JpaRepository<SongCredit, Long> {
    List<SongCredit> findBySongIdOrderBySortOrderAscIdAsc(Long songId);

    @Modifying
    @Query("delete from SongCredit credit where credit.song.id = :songId")
    void deleteBySongId(Long songId);
}
