package com.each17.backend.vocabulary.repository;

import com.each17.backend.vocabulary.entity.VocabularyOverride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyOverrideRepository extends JpaRepository<VocabularyOverride, String> {
}
