package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.VocabularyQualityCandidateDto;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricNormalizer;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.vocabulary.entity.VocabularyOverride;
import com.each17.backend.vocabulary.repository.VocabularyOverrideRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final VocabularyOverrideRepository overrideRepository;
    private final LyricTokenizationService tokenizationService;
    private final EnglishLemmaService lemmaService;
    private final ObjectMapper objectMapper;
    private final VocabularyRebuildTaskRegistry rebuildTaskRegistry;
    private final VocabularyRebuildWorker rebuildWorker;

    // ---------- 对外接口 ----------
    @Override
    public WordPageDto getWordList(
            String prefix,
            int page,
            int size,
            boolean recommendedOnly,
            boolean lemmaSearch
    ) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ValidationException("Page must be >= 0 and size must be between 1 and 200");
        }
        // 创建按单词字母顺序排序的分页请求
        Pageable pageable = PageRequest.of(page, size);
        Page<Vocabulary> vocabularyPage;

        String normalizedPrefix = normalizeSearchPrefix(prefix, lemmaSearch);
        if (normalizedPrefix != null) {
            if (recommendedOnly) {
                vocabularyPage = vocabularyRepository.findByRecommendedTrueAndWordStartingWithOrderByWordAsc(normalizedPrefix, pageable);
            } else {
                vocabularyPage = vocabularyRepository.findByWordStartingWithOrderByWordAsc(normalizedPrefix, pageable);
            }
        } else {
            if (recommendedOnly) {
                vocabularyPage = vocabularyRepository.findByRecommendedTrueOrderByWordAsc(pageable);
            } else {
                vocabularyPage = vocabularyRepository.findAllByOrderByWordAsc(pageable);
            }
        }

        // 直接从数据库获取已排序的单词列表
        List<String> words = vocabularyPage.getContent().stream()
                .map(Vocabulary::getWord)
                .toList();

        return WordPageDto.builder()
                .content(words)
                .totalElements(vocabularyPage.getTotalElements())
                .totalPages(vocabularyPage.getTotalPages())
                .number(vocabularyPage.getNumber())
                .size(vocabularyPage.getSize())
                .build();
    }

    @Override
    public List<WordOccurrenceDto> getWordOccurrences(String word) {
        String lemma = normalizeLookupWord(word);
        Optional<Vocabulary> vocabOpt = vocabularyRepository.findById(lemma);
        if (vocabOpt.isEmpty()) {
            throw new NotFoundException("Word not found: " + word);
        }

        try {
            // [核心实现]
            Vocabulary vocabulary = vocabOpt.get();
            List<WordOccurrenceDto> occurrences = objectMapper.readValue(vocabulary.getOccurrences(), new TypeReference<>() {});
            occurrences.forEach(occurrence -> sanitizeOccurrence(occurrence, vocabulary.getLearningScore()));
            return occurrences;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize occurrences for lemma: {}", lemma, e);
            throw new RuntimeException("Failed to deserialize occurrences for word: " + word, e);
        }
    }

    @Override
    public List<VocabularyQualityCandidateDto> getQualityCandidates(int limit) {
        if (limit < 1 || limit > 200) {
            throw new ValidationException("limit must be between 1 and 200");
        }

        Page<Vocabulary> cleanupPage = vocabularyRepository.findCleanupCandidates(PageRequest.of(0, limit));
        if (cleanupPage != null) {
            return cleanupPage.getContent().stream()
                    .map(this::toQualityCandidate)
                    .filter(item -> !item.getReasons().isEmpty())
                    .toList();
        }

        // Keep compatibility with lightweight repository mocks that predate the optimized query.
        Map<String, Vocabulary> candidates = new LinkedHashMap<>();
        Pageable pageable = PageRequest.of(0, limit);
        vocabularyRepository.findByRecommendedFalseOrderByLearningScoreAscWordAsc(pageable)
                .forEach(item -> candidates.put(item.getWord(), item));

        return candidates.values().stream()
                .map(this::toQualityCandidate)
                .filter(item -> !item.getReasons().isEmpty())
                .sorted(Comparator
                        .comparing(VocabularyQualityCandidateDto::getLearningScore)
                        .thenComparing(VocabularyQualityCandidateDto::getWord))
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    public int deleteWords(List<String> words) {
        if (words == null || words.isEmpty() || words.size() > 200) {
            throw new ValidationException("words must contain between 1 and 200 items");
        }

        List<String> normalizedWords = words.stream()
                .map(this::normalizeLookupWord)
                .filter(word -> !word.isBlank())
                .distinct()
                .toList();

        if (normalizedWords.isEmpty()) {
            throw new ValidationException("words must contain at least one valid word");
        }

        List<Vocabulary> existingWords = vocabularyRepository.findAllById(normalizedWords);
        List<VocabularyOverride> overrides = existingWords.stream()
                .map(vocabulary -> VocabularyOverride.builder()
                        .lemma(vocabulary.getWord())
                        .excluded(true)
                        .recommendedOverride(false)
                        .updatedAt(LocalDateTime.now().toString())
                        .build())
                .toList();
        overrideRepository.saveAll(overrides);
        vocabularyRepository.deleteAllInBatch(existingWords);
        return existingWords.size();
    }

    @Override
    @Transactional
    public VocabularyQualityCandidateDto updateLearningValue(String word, boolean recommended) {
        String lemma = normalizeLookupWord(word);
        Vocabulary vocabulary = vocabularyRepository.findById(lemma)
                .orElseThrow(() -> new NotFoundException("Word not found: " + word));

        vocabulary.setRecommended(recommended);
        double currentScore = vocabulary.getLearningScore() == null ? 0.0 : vocabulary.getLearningScore();
        vocabulary.setLearningScore(recommended ? Math.max(currentScore, 1.0) : 0.25);
        Vocabulary saved = vocabularyRepository.save(vocabulary);
        VocabularyOverride override = overrideRepository.findById(lemma)
                .orElseGet(() -> VocabularyOverride.builder().lemma(lemma).excluded(false).build());
        override.setExcluded(false);
        override.setRecommendedOverride(recommended);
        override.setUpdatedAt(LocalDateTime.now().toString());
        overrideRepository.save(override);
        return toQualityCandidate(saved);
    }

    @Override
    public UUID refreshVocabularyIndexAsync() {
        VocabularyRebuildTaskRegistry.Submission submission = rebuildTaskRegistry.submit();
        if (submission.newlyCreated()) rebuildWorker.rebuild(submission.taskId());
        return submission.taskId();
    }

    @Override
    public VocabularyRebuildTaskDto getRefreshTaskStatus(UUID taskId) {
        VocabularyRebuildTaskDto task = rebuildTaskRegistry.get(taskId);
        if (task == null) {
            throw new NotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    private String normalizeSearchPrefix(String prefix, boolean lemmaSearch) {
        if (prefix == null || prefix.isBlank()) return null;
        String normalized = tokenizationService.normalize(prefix);
        return lemmaSearch ? lemmaService.lemma(normalized) : normalized;
    }

    private String normalizeLookupWord(String word) {
        if (word == null) return "";
        String normalized = tokenizationService.normalizeToLemmaPhrase(word);
        return normalized.isBlank() ? lemmaService.lemma(tokenizationService.normalize(word)) : normalized;
    }

    private VocabularyQualityCandidateDto toQualityCandidate(Vocabulary vocabulary) {
        return VocabularyQualityCandidateDto.builder()
                .word(vocabulary.getWord())
                .learningScore(vocabulary.getLearningScore())
                .occurrenceCount(vocabulary.getOccurrenceCount())
                .songCount(vocabulary.getSongCount())
                .recommended(vocabulary.getRecommended())
                .reasons(qualityReasons(vocabulary))
                .examples(readOccurrenceExamples(vocabulary, 2))
                .build();
    }

    private List<String> qualityReasons(Vocabulary vocabulary) {
        String word = vocabulary.getWord();
        List<String> reasons = new ArrayList<>();
        if (Boolean.FALSE.equals(vocabulary.getRecommended()) || vocabulary.getLearningScore() < 0.5) {
            reasons.add("LOW_LEARNING_VALUE");
        }
        if (word.matches(".*\\b[a-z]{2,}v\\b.*") || word.matches(".*\\b[a-z]{2,}av\\b.*")) {
            reasons.add("POSSIBLE_TRUNCATED_LEMMA");
        }
        if (word.length() <= 2) {
            reasons.add("VERY_SHORT_TOKEN");
        }
        if (!word.matches("[a-z]+(?:'[a-z]+)?(?: [a-z]+(?:'[a-z]+)?)*")) {
            reasons.add("NON_STANDARD_TOKEN");
        }
        return reasons;
    }

    private List<WordOccurrenceDto> readOccurrenceExamples(Vocabulary vocabulary, int limit) {
        try {
            List<WordOccurrenceDto> occurrences = objectMapper.readValue(vocabulary.getOccurrences(), new TypeReference<>() {});
            return occurrences == null ? List.of() : occurrences.stream()
                    .limit(limit)
                    .map(occurrence -> {
                        sanitizeOccurrence(occurrence, vocabulary.getLearningScore());
                        return occurrence;
                    })
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize candidate occurrences for lemma: {}", vocabulary.getWord(), e);
            return List.of();
        }
    }

    private void sanitizeOccurrence(WordOccurrenceDto occurrence, Double learningScore) {
        occurrence.setLyricLine(LyricNormalizer.removeTimestamps(occurrence.getLyricLine()));
        occurrence.setLearningScore(learningScore);
    }
}
