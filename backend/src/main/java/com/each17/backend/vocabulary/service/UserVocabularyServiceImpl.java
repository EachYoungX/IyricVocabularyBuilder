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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class UserVocabularyServiceImpl implements UserVocabularyService {
    private static final String LOCAL_USER_ID = "local";
    private static final Set<VocabularyStatus> NON_REVIEWABLE = Set.of(
            VocabularyStatus.MASTERED, VocabularyStatus.BOOKMARK_ONLY, VocabularyStatus.IGNORED);

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
            applyStatus(vocabulary, request.getStatus());
        }
        if (request.getStatus() == null && request.getMasteryScore() != null) {
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
    @Transactional
    public List<UserVocabularyDto> updateWords(List<UserVocabularyBatchUpdateDto> requests) {
        if (requests == null || requests.isEmpty() || requests.size() > 500) {
            throw new ValidationException("batch update must contain between 1 and 500 items");
        }
        Map<Long, UserVocabularyBatchUpdateDto> byId = requests.stream()
                .filter(request -> request != null && request.getId() != null)
                .collect(Collectors.toMap(UserVocabularyBatchUpdateDto::getId, Function.identity(),
                        (left, right) -> right, LinkedHashMap::new));
        if (byId.size() != requests.size()) throw new ValidationException("Every batch update requires a unique id");

        List<UserVocabulary> words = userVocabularyRepository.findAllById(byId.keySet());
        validateOwnedIds(byId.keySet(), words);
        String timestamp = now();
        for (UserVocabulary word : words) {
            UserVocabularyBatchUpdateDto request = byId.get(word.getId());
            if (request.getStatus() != null) applyStatus(word, request.getStatus());
            if (request.getNote() != null) word.setNote(request.getNote());
            word.setLastSeenAt(timestamp);
        }
        return userVocabularyRepository.saveAll(words).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<UserVocabularyDto> importWords(List<UserVocabularyImportItemDto> requests) {
        if (requests == null || requests.isEmpty() || requests.size() > 2_000) {
            throw new ValidationException("import must contain between 1 and 2000 items");
        }
        Map<String, UserVocabularyImportItemDto> byLemma = new LinkedHashMap<>();
        for (UserVocabularyImportItemDto request : requests) {
            String lemma = normalizeLemma(request == null ? null : request.getLemma());
            byLemma.put(lemma, request);
        }
        Map<String, UserVocabulary> existing = userVocabularyRepository
                .findByUserIdAndLemmaIn(LOCAL_USER_ID, byLemma.keySet()).stream()
                .collect(Collectors.toMap(UserVocabulary::getLemma, Function.identity()));
        String timestamp = now();
        List<UserVocabulary> words = byLemma.entrySet().stream().map(entry -> {
            UserVocabularyImportItemDto request = entry.getValue();
            UserVocabulary word = existing.getOrDefault(entry.getKey(), UserVocabulary.builder()
                    .userId(LOCAL_USER_ID).lemma(entry.getKey()).status(VocabularyStatus.NEW)
                    .masteryScore(0.0).firstSeenAt(timestamp).build());
            if (request.getStatus() != null) applyStatus(word, request.getStatus());
            else if (word.getReviewDueAt() == null) word.setReviewDueAt(timestamp);
            if (request.getNote() != null) word.setNote(request.getNote());
            word.setLastSeenAt(timestamp);
            return word;
        }).toList();
        return userVocabularyRepository.saveAll(words).stream().map(this::toDto).toList();
    }

    @Override
    public UserVocabularyStatsDto getStats() {
        List<UserVocabularyDto> recentWords = userVocabularyRepository.findTop8ByUserIdOrderByLastSeenAtDesc(LOCAL_USER_ID)
                .stream()
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
        List<UserVocabulary> due = userVocabularyRepository.findDueReviews(
                LOCAL_USER_ID, NON_REVIEWABLE, now(), PageRequest.of(0, limit));
        Map<String, Vocabulary> indexed = vocabularyRepository.findAllById(
                        due.stream().map(UserVocabulary::getLemma).toList()).stream()
                .collect(Collectors.toMap(Vocabulary::getWord, Function.identity()));
        return due.stream()
                .map(word -> toReviewItem(word, indexed.get(word.getLemma())))
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
    public void deleteWords(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 500) {
            throw new ValidationException("batch delete must contain between 1 and 500 ids");
        }
        List<Long> uniqueIds = ids.stream().distinct().toList();
        if (uniqueIds.size() != ids.size()) throw new ValidationException("batch delete ids must be unique");
        List<UserVocabulary> words = userVocabularyRepository.findAllById(uniqueIds);
        validateOwnedIds(uniqueIds, words);
        userVocabularyRepository.deleteAllInBatch(words);
    }

    @Override
    @Transactional
    public void clearAllWords() {
        userVocabularyRepository.deleteByUserId(LOCAL_USER_ID);
    }

    @Override
    @Transactional
    public void addDefaultWordsForSong(Long songId) {
        List<String> lemmas = lyricTokenRepository.findDistinctByLyricLineSongIdAndLearningScoreGreaterThan(songId, 0.5)
                .stream()
                .map(LyricToken::getLemma)
                .filter(lemma -> lemma != null && !lemma.isBlank())
                .distinct()
                .toList();
        if (lemmas.isEmpty()) return;
        Set<String> existing = userVocabularyRepository.findByUserIdAndLemmaIn(LOCAL_USER_ID, lemmas).stream()
                .map(UserVocabulary::getLemma).collect(Collectors.toSet());
        String timestamp = now();
        List<UserVocabulary> missing = lemmas.stream().filter(lemma -> !existing.contains(lemma))
                .map(lemma -> UserVocabulary.builder().userId(LOCAL_USER_ID).lemma(lemma)
                        .status(VocabularyStatus.NEW).masteryScore(0.0).firstSeenAt(timestamp)
                        .lastSeenAt(timestamp).reviewDueAt(timestamp).build())
                .toList();
        if (!missing.isEmpty()) userVocabularyRepository.saveAll(missing);
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
        return userVocabularyRepository.countDueReviews(LOCAL_USER_ID, NON_REVIEWABLE, now());
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

    private UserVocabularyReviewItemDto toReviewItem(UserVocabulary vocabulary, Vocabulary indexedVocabulary) {
        return UserVocabularyReviewItemDto.builder()
                .id(vocabulary.getId())
                .lemma(vocabulary.getLemma())
                .status(vocabulary.getStatus())
                .masteryScore(vocabulary.getMasteryScore())
                .reviewDueAt(vocabulary.getReviewDueAt())
                .example(indexedVocabulary == null ? null : readFirstOccurrence(indexedVocabulary).orElse(null))
                .build();
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

    private void applyStatus(UserVocabulary vocabulary, VocabularyStatus status) {
        vocabulary.setStatus(status);
        vocabulary.setMasteryScore(switch (status) {
            case NEW, BOOKMARK_ONLY, IGNORED -> 0.0;
            case LEARNING -> 0.25;
            case FAMILIAR -> 0.6;
            case MASTERED -> 1.0;
        });
        vocabulary.setReviewDueAt(nextReviewDueAt(status));
    }

    private void validateOwnedIds(Collection<Long> requestedIds, List<UserVocabulary> words) {
        Set<Long> found = words.stream()
                .filter(item -> LOCAL_USER_ID.equals(item.getUserId()))
                .map(UserVocabulary::getId)
                .collect(Collectors.toSet());
        List<Long> missing = requestedIds.stream().filter(id -> !found.contains(id)).toList();
        if (!missing.isEmpty()) throw new NotFoundException("User vocabulary not found: " + missing);
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
