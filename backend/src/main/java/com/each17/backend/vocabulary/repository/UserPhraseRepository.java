package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.UserPhrase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPhraseRepository extends JpaRepository<UserPhrase, Long> {
    List<UserPhrase> findByUserIdOrderByCanonicalPhraseAsc(String userId);
}
