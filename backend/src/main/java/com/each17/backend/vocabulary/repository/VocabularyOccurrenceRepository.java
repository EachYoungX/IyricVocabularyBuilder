package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.VocabularyOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyOccurrenceRepository extends JpaRepository<VocabularyOccurrence, Long> {
}
