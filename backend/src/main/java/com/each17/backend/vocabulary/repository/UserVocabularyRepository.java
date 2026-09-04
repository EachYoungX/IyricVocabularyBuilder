package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.UserVocabulary;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    Optional<UserVocabulary> findByUserIdAndLemma(String userId, String lemma);
    List<UserVocabulary> findByUserIdOrderByLastSeenAtDesc(String userId);
    List<UserVocabulary> findTop8ByUserIdOrderByLastSeenAtDesc(String userId);
    List<UserVocabulary> findByUserIdAndStatusOrderByLastSeenAtDesc(String userId, VocabularyStatus status);
    List<UserVocabulary> findByUserIdAndLemmaIn(String userId, Collection<String> lemmas);

    @Query("""
            select word from UserVocabulary word
            where word.userId = :userId
              and word.status not in :excludedStatuses
              and (word.reviewDueAt is null or word.reviewDueAt <= :timestamp)
            order by word.reviewDueAt asc, word.id asc
            """)
    List<UserVocabulary> findDueReviews(
            String userId, Collection<VocabularyStatus> excludedStatuses, String timestamp, Pageable pageable);

    @Query("""
            select count(word) from UserVocabulary word
            where word.userId = :userId
              and word.status not in :excludedStatuses
              and (word.reviewDueAt is null or word.reviewDueAt <= :timestamp)
            """)
    long countDueReviews(String userId, Collection<VocabularyStatus> excludedStatuses, String timestamp);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, VocabularyStatus status);
    void deleteByUserId(String userId);
}
