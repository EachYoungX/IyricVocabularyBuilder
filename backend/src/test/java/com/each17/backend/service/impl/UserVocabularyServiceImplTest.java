package com.each17.backend.service.impl;

import com.each17.backend.dto.UserVocabularyRequestDto;
import com.each17.backend.dto.UserVocabularyUpdateRequestDto;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.vocabulary.entity.UserVocabulary;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import com.each17.backend.vocabulary.repository.UserVocabularyRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.each17.backend.vocabulary.service.UserVocabularyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserVocabularyServiceImplTest {
    @Mock
    private UserVocabularyRepository userVocabularyRepository;
    @Mock
    private VocabularyRepository vocabularyRepository;
    @Mock
    private LyricTokenizationService tokenizationService;

    private UserVocabularyServiceImpl userVocabularyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tokenizationService.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toLowerCase());
        EnglishLemmaService lemmaService = new EnglishLemmaService();
        when(tokenizationService.normalizeToLemmaPhrase(anyString())).thenAnswer(invocation ->
                Arrays.stream(invocation.getArgument(0, String.class).toLowerCase().split("\\s+"))
                        .map(lemmaService::lemma)
                        .collect(Collectors.joining(" "))
        );
        userVocabularyService = new UserVocabularyServiceImpl(
                userVocabularyRepository,
                vocabularyRepository,
                tokenizationService,
                lemmaService,
                new ObjectMapper()
        );
    }

    @Test
    void addWordCreatesNewLocalVocabulary() {
        when(userVocabularyRepository.findByUserIdAndLemma("local", "run")).thenReturn(Optional.empty());
        when(userVocabularyRepository.save(any(UserVocabulary.class))).thenAnswer(invocation -> {
            UserVocabulary saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = userVocabularyService.addWord(UserVocabularyRequestDto.builder()
                .lemma("Running")
                .note("appears in chorus")
                .build());

        assertEquals("run", result.getLemma());
        assertEquals(VocabularyStatus.NEW, result.getStatus());
        assertEquals(0.0, result.getMasteryScore());
        assertEquals("appears in chorus", result.getNote());
    }

    @Test
    void addWordCreatesPhraseVocabulary() {
        when(userVocabularyRepository.findByUserIdAndLemma("local", "silver lining")).thenReturn(Optional.empty());
        when(userVocabularyRepository.save(any(UserVocabulary.class))).thenAnswer(invocation -> {
            UserVocabulary saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        var result = userVocabularyService.addWord(UserVocabularyRequestDto.builder()
                .lemma("Silver Linings")
                .build());

        assertEquals("silver lining", result.getLemma());
    }

    @Test
    void updateWordAdvancesStatusAndMasteryScore() {
        UserVocabulary existing = UserVocabulary.builder()
                .id(1L)
                .userId("local")
                .lemma("run")
                .status(VocabularyStatus.NEW)
                .masteryScore(0.0)
                .firstSeenAt("2026-06-30T10:00:00")
                .lastSeenAt("2026-06-30T10:00:00")
                .reviewDueAt("2026-06-30T10:00:00")
                .build();
        when(userVocabularyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userVocabularyRepository.save(any(UserVocabulary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userVocabularyService.updateWord(1L, UserVocabularyUpdateRequestDto.builder()
                .status(VocabularyStatus.LEARNING)
                .masteryScore(0.4)
                .note("needs review")
                .build());

        assertEquals(VocabularyStatus.LEARNING, result.getStatus());
        assertEquals(0.4, result.getMasteryScore());
        assertEquals("needs review", result.getNote());
        assertNotNull(result.getReviewDueAt());
    }

    @Test
    void reviewQueueSkipsMasteredBookmarkOnlyAndIgnoredWords() {
        when(userVocabularyRepository.findByUserIdOrderByLastSeenAtDesc("local")).thenReturn(List.of(
                item(1L, "run", VocabularyStatus.NEW, "2026-01-01T00:00:00"),
                item(2L, "sing", VocabularyStatus.MASTERED, "2026-01-01T00:00:00"),
                item(3L, "keep", VocabularyStatus.BOOKMARK_ONLY, "2026-01-01T00:00:00"),
                item(4L, "hide", VocabularyStatus.IGNORED, "2026-01-01T00:00:00")
        ));

        var result = userVocabularyService.getReviewQueue(10);

        assertEquals(1, result.size());
        assertEquals("run", result.getFirst().getLemma());
    }

    @Test
    void clearAllWordsDeletesLocalVocabulary() {
        userVocabularyService.clearAllWords();

        verify(userVocabularyRepository).deleteByUserId("local");
    }

    private UserVocabulary item(Long id, String lemma, VocabularyStatus status, String reviewDueAt) {
        return UserVocabulary.builder()
                .id(id)
                .userId("local")
                .lemma(lemma)
                .status(status)
                .masteryScore(0.0)
                .firstSeenAt("2026-01-01T00:00:00")
                .lastSeenAt("2026-01-01T00:00:00")
                .reviewDueAt(reviewDueAt)
                .build();
    }
}
