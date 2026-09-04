package com.each17.backend.vocabulary.service;

import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.vocabulary.entity.VocabularyOverride;
import com.each17.backend.vocabulary.repository.VocabularyOverrideRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyRebuildTransaction {
    private final VocabularyRepository vocabularyRepository;
    private final VocabularyOverrideRepository overrideRepository;

    @Transactional
    public int replace(List<Vocabulary> generated) {
        Map<String, VocabularyOverride> overrides = overrideRepository.findAll().stream()
                .collect(Collectors.toMap(VocabularyOverride::getLemma, Function.identity()));
        List<Vocabulary> effective = generated.stream()
                .filter(vocabulary -> !isExcluded(overrides.get(vocabulary.getWord())))
                .peek(vocabulary -> applyOverride(vocabulary, overrides.get(vocabulary.getWord())))
                .toList();

        vocabularyRepository.deleteAllInBatch();
        if (!effective.isEmpty()) vocabularyRepository.saveAll(effective);
        return effective.size();
    }

    private boolean isExcluded(VocabularyOverride override) {
        return override != null && Boolean.TRUE.equals(override.getExcluded());
    }

    private void applyOverride(Vocabulary vocabulary, VocabularyOverride override) {
        if (override == null || override.getRecommendedOverride() == null) return;
        boolean recommended = override.getRecommendedOverride();
        vocabulary.setRecommended(recommended);
        double score = vocabulary.getLearningScore() == null ? 0.0 : vocabulary.getLearningScore();
        vocabulary.setLearningScore(recommended ? Math.max(score, 1.0) : 0.25);
    }
}
