package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, String> {
    // 查询以某个前缀开头的单词，并支持分页，按单词字母顺序排序
    Page<Vocabulary> findByRecommendedTrueAndWordStartingWithOrderByWordAsc(String prefix, Pageable pageable);
    Page<Vocabulary> findByRecommendedTrueAndWordStartingWithAndWordNotContainingOrderByWordAsc(String prefix, String excluded, Pageable pageable);
    Page<Vocabulary> findByWordStartingWithOrderByWordAsc(String prefix, Pageable pageable);
    Page<Vocabulary> findByWordStartingWithAndWordNotContainingOrderByWordAsc(String prefix, String excluded, Pageable pageable);
    
    // 查询所有单词，按单词字母顺序排序
    Page<Vocabulary> findByRecommendedTrueOrderByWordAsc(Pageable pageable);
    Page<Vocabulary> findByRecommendedTrueAndWordNotContainingOrderByWordAsc(String excluded, Pageable pageable);
    Page<Vocabulary> findAllByOrderByWordAsc(Pageable pageable);
    Page<Vocabulary> findByWordNotContainingOrderByWordAsc(String excluded, Pageable pageable);
    List<Vocabulary> findByRecommendedFalseOrderByLearningScoreAscWordAsc(Pageable pageable);

    @Query("SELECT v FROM Vocabulary v "
            + "WHERE v.recommended = false OR v.learningScore < 0.5 "
            + "ORDER BY v.learningScore ASC, v.word ASC")
    Page<Vocabulary> findCleanupCandidates(Pageable pageable);
}
