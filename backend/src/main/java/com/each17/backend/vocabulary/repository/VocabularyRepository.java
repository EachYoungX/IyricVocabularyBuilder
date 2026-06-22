package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, String> {
    // 查询以某个前缀开头的单词，并支持分页，按单词字母顺序排序
    Page<Vocabulary> findByWordStartingWithOrderByWordAsc(String prefix, Pageable pageable);
    
    // 查询所有单词，按单词字母顺序排序
    Page<Vocabulary> findAllByOrderByWordAsc(Pageable pageable);
}
