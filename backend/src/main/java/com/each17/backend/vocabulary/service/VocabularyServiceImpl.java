package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.VocabularyQualityCandidateDto;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.song.entity.Song;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final SongRepository songRepository;
    private final LyricTokenizationService tokenizationService;
    private final EnglishLemmaService lemmaService;
    private final VocabularyIndexBuilder vocabularyIndexBuilder;
    private final ObjectMapper objectMapper;

    // 任务状态存储（内存，生产环境可以换成 Redis）
    private final ConcurrentMap<UUID, VocabularyRebuildTaskDto> rebuildTasks = new ConcurrentHashMap<>();
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
            occurrences.forEach(occurrence -> occurrence.setLearningScore(vocabulary.getLearningScore()));
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
        return toQualityCandidate(vocabularyRepository.save(vocabulary));
    }

    @Override
    public UUID refreshVocabularyIndexAsync() {
        UUID taskId = UUID.randomUUID();
        VocabularyRebuildTaskDto task = VocabularyRebuildTaskDto.builder()
                .taskId(taskId)
                .status("PENDING")
                .startedAt(LocalDateTime.now())
                .build();
        rebuildTasks.put(taskId, task);
        rebuildVocabularyInBackground(taskId);
        return taskId;
    }

    @Override
    public VocabularyRebuildTaskDto getRefreshTaskStatus(UUID taskId) {
        VocabularyRebuildTaskDto task = rebuildTasks.get(taskId);
        if (task == null) {
            throw new NotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    @Async
    @Transactional
    public void rebuildVocabularyInBackground(UUID taskId) {
        VocabularyRebuildTaskDto task = rebuildTasks.get(taskId);
        if (task == null) return;

        task.setStatus("RUNNING");
        log.info("[Refresh Task {}] Rebuilding vocabulary index...", taskId);

        try {
            List<Song> allSongs = songRepository.findAll();
            List<Vocabulary> entities = vocabularyIndexBuilder.rebuildFromSongs(allSongs);

            // 原子替换
            vocabularyRepository.deleteAllInBatch();
            if (!entities.isEmpty()) {
                vocabularyRepository.saveAll(entities);
            }

            task.setStatus("COMPLETED");
            log.info("[Refresh Task {}] Vocabulary rebuilt successfully. {} words.", taskId, entities.size());

        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            log.error("[Refresh Task {}] Rebuild failed.", taskId, e);
        } finally {
            task.setFinishedAt(LocalDateTime.now());
            // 可选：任务结束 30 分钟后自动清理内存
            // refreshTasks.remove(taskId);
        }
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
            return occurrences == null ? List.of() : occurrences.stream().limit(limit).toList();
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize candidate occurrences for lemma: {}", vocabulary.getWord(), e);
            return List.of();
        }
    }
}
