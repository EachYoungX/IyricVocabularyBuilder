package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.*;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.vocabulary.entity.UserVocabulary;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import com.each17.backend.vocabulary.repository.UserVocabularyRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class UserVocabularyServiceImpl implements UserVocabularyService {
    private static final String LOCAL_USER_ID = "local";

    private final UserVocabularyRepository userVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final LyricTokenizationService tokenizationService;
    private final EnglishLemmaService lemmaService;
    private final ObjectMapper objectMapper;
    private final LyricTokenRepository lyricTokenRepository;

    // Keeps lightweight unit-test construction compatible with the pre-import-automation service shape.
    public UserVocabularyServiceImpl(
            UserVocabularyRepository userVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            LyricTokenizationService tokenizationService,
            EnglishLemmaService lemmaService,
            ObjectMapper objectMapper
    ) {
        this(userVocabularyRepository, vocabularyRepository, tokenizationService, lemmaService, objectMapper, null);
    }

    @Override
    @Transactional
    public UserVocabularyDto addWord(UserVocabularyRequestDto request) {
        String lemma = normalizeLemma(request == null ? null : request.getLemma());
        Optional<UserVocabulary> existing = userVocabularyRepository.findByUserIdAndLemma(LOCAL_USER_ID, lemma);
        if (existing.isPresent()) {
            UserVocabulary vocabulary = existing.get();
            if (request != null && request.getNote() != null) {
                vocabulary.setNote(request.getNote());
            }
            vocabulary.setLastSeenAt(now());
            return toDto(userVocabularyRepository.save(vocabulary));
        }

        String timestamp = now();
        UserVocabulary vocabulary = UserVocabulary.builder()
                .userId(LOCAL_USER_ID)
                .lemma(lemma)
                .status(VocabularyStatus.NEW)
                .masteryScore(0.0)
                .firstSeenAt(timestamp)
                .lastSeenAt(timestamp)
                .reviewDueAt(timestamp)
                .note(request == null ? null : request.getNote())
                .build();
        return toDto(userVocabularyRepository.save(vocabulary));
    }

    @Override
    public List<UserVocabularyDto> listWords(VocabularyStatus status) {
        List<UserVocabulary> words = status == null
                ? userVocabularyRepository.findByUserIdOrderByLastSeenAtDesc(LOCAL_USER_ID)
                : userVocabularyRepository.findByUserIdAndStatusOrderByLastSeenAtDesc(LOCAL_USER_ID, status);
        return words.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public UserVocabularyDto updateWord(Long id, UserVocabularyUpdateRequestDto request) {
        UserVocabulary vocabulary = userVocabularyRepository.findById(id)
                .filter(item -> LOCAL_USER_ID.equals(item.getUserId()))
                .orElseThrow(() -> new NotFoundException("User vocabulary not found: " + id));

        if (request.getStatus() != null) {
            vocabulary.setStatus(request.getStatus());
            vocabulary.setReviewDueAt(nextReviewDueAt(request.getStatus()));
        }
        if (request.getMasteryScore() != null) {
            if (request.getMasteryScore() < 0 || request.getMasteryScore() > 1) {
                throw new ValidationException("masteryScore must be between 0 and 1");
            }
            vocabulary.setMasteryScore(request.getMasteryScore());
        }
        if (request.getNote() != null) {
            vocabulary.setNote(request.getNote());
        }
        vocabulary.setLastSeenAt(now());
        return toDto(userVocabularyRepository.save(vocabulary));
    }

    @Override
    public UserVocabularyStatsDto getStats() {
        List<UserVocabularyDto> recentWords = userVocabularyRepository.findByUserIdOrderByLastSeenAtDesc(LOCAL_USER_ID)
                .stream()
                .limit(8)
                .map(this::toDto)
                .toList();
        return UserVocabularyStatsDto.builder()
                .totalCount(userVocabularyRepository.countByUserId(LOCAL_USER_ID))
                .newCount(userVocabularyRepository.countByUserIdAndStatus(LOCAL_USER_ID, VocabularyStatus.NEW))
                .learningCount(userVocabularyRepository.countByUserIdAndStatus(LOCAL_USER_ID, VocabularyStatus.LEARNING))
                .familiarCount(userVocabularyRepository.countByUserIdAndStatus(LOCAL_USER_ID, VocabularyStatus.FAMILIAR))
                .masteredCount(userVocabularyRepository.countByUserIdAndStatus(LOCAL_USER_ID, VocabularyStatus.MASTERED))
                .ignoredCount(userVocabularyRepository.countByUserIdAndStatus(LOCAL_USER_ID, VocabularyStatus.IGNORED))
                .dueReviewCount(countDueReviews())
                .recentWords(recentWords)
                .build();
    }

    @Override
    public List<UserVocabularyReviewItemDto> getReviewQueue(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ValidationException("limit must be between 1 and 100");
        }
        String timestamp = now();
        return userVocabularyRepository.findByUserIdOrderByLastSeenAtDesc(LOCAL_USER_ID).stream()
                .filter(this::isReviewable)
                .filter(item -> item.getReviewDueAt() == null || item.getReviewDueAt().compareTo(timestamp) <= 0)
                .sorted(Comparator.comparing(item -> item.getReviewDueAt() == null ? "" : item.getReviewDueAt()))
                .limit(limit)
                .map(this::toReviewItem)
                .toList();
    }

    @Override
    @Transactional
    public void deleteWord(Long id) {
        UserVocabulary vocabulary = userVocabularyRepository.findById(id)
                .filter(item -> LOCAL_USER_ID.equals(item.getUserId()))
                .orElseThrow(() -> new NotFoundException("User vocabulary not found: " + id));
        userVocabularyRepository.delete(vocabulary);
    }

    @Override
    @Transactional
    public void clearAllWords() {
        userVocabularyRepository.deleteByUserId(LOCAL_USER_ID);
    }

    @Override
    @Transactional
    public void addDefaultWordsForSong(Long songId) {
        lyricTokenRepository.findDistinctByLyricLineSongIdAndLearningScoreGreaterThan(songId, 0.5)
                .stream()
                .map(LyricToken::getLemma)
                .filter(lemma -> lemma != null && !lemma.isBlank())
                .distinct()
                .forEach(lemma -> addWord(UserVocabularyRequestDto.builder().lemma(lemma).build()));
    }

    private String normalizeLemma(String rawWord) {
        if (rawWord == null || rawWord.isBlank()) {
            throw new ValidationException("lemma is required");
        }
        String lemma = tokenizationService.normalizeToLemmaPhrase(rawWord);
        if (lemma == null || lemma.isBlank()) {
            throw new ValidationException("lemma must contain an English word");
        }
        return lemma;
    }

    private long countDueReviews() {
        String timestamp = now();
        return userVocabularyRepository.findByUserIdOrderByLastSeenAtDesc(LOCAL_USER_ID).stream()
                .filter(this::isReviewable)
                .filter(item -> item.getReviewDueAt() == null || item.getReviewDueAt().compareTo(timestamp) <= 0)
                .count();
    }

    private UserVocabularyDto toDto(UserVocabulary vocabulary) {
        return UserVocabularyDto.builder()
                .id(vocabulary.getId())
                .userId(vocabulary.getUserId())
                .lemma(vocabulary.getLemma())
                .status(vocabulary.getStatus())
                .masteryScore(vocabulary.getMasteryScore())
                .firstSeenAt(vocabulary.getFirstSeenAt())
                .lastSeenAt(vocabulary.getLastSeenAt())
                .reviewDueAt(vocabulary.getReviewDueAt())
                .note(vocabulary.getNote())
                .build();
    }

    private UserVocabularyReviewItemDto toReviewItem(UserVocabulary vocabulary) {
        return UserVocabularyReviewItemDto.builder()
                .id(vocabulary.getId())
                .lemma(vocabulary.getLemma())
                .status(vocabulary.getStatus())
                .masteryScore(vocabulary.getMasteryScore())
                .reviewDueAt(vocabulary.getReviewDueAt())
                .example(findFirstOccurrence(vocabulary.getLemma()))
                .build();
    }

    private WordOccurrenceDto findFirstOccurrence(String lemma) {
        return vocabularyRepository.findById(lemma)
                .flatMap(this::readFirstOccurrence)
                .orElse(null);
    }

    private Optional<WordOccurrenceDto> readFirstOccurrence(Vocabulary vocabulary) {
        try {
            List<WordOccurrenceDto> occurrences = objectMapper.readValue(vocabulary.getOccurrences(), new TypeReference<>() {});
            return occurrences.stream().findFirst();
        } catch (JsonProcessingException e) {
            log.warn("Failed to read review occurrence for lemma: {}", vocabulary.getWord(), e);
            return Optional.empty();
        }
    }

    private String nextReviewDueAt(VocabularyStatus status) {
        LocalDateTime base = LocalDateTime.now();
        return switch (status) {
            case NEW -> base.toString();
            case LEARNING -> base.plusDays(1).toString();
            case FAMILIAR -> base.plusDays(3).toString();
            case MASTERED -> base.plusDays(14).toString();
            case BOOKMARK_ONLY -> null;
            case IGNORED -> null;
        };
    }

    private boolean isReviewable(UserVocabulary item) {
        return item.getStatus() != VocabularyStatus.MASTERED
                && item.getStatus() != VocabularyStatus.BOOKMARK_ONLY
                && item.getStatus() != VocabularyStatus.IGNORED;
    }

    private String now() {
        return LocalDateTime.now().toString();
    }
}
