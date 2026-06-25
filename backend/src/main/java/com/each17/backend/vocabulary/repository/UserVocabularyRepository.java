package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.UserVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    Optional<UserVocabulary> findByUserIdAndLemma(String userId, String lemma);
}
