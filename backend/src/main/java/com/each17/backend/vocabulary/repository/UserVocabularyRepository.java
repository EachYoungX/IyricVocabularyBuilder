package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.UserVocabulary;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    Optional<UserVocabulary> findByUserIdAndLemma(String userId, String lemma);
    List<UserVocabulary> findByUserIdOrderByLastSeenAtDesc(String userId);
    List<UserVocabulary> findByUserIdAndStatusOrderByLastSeenAtDesc(String userId, VocabularyStatus status);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, VocabularyStatus status);
    void deleteByUserId(String userId);
}
