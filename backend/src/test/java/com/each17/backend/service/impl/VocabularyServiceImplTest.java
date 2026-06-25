package com.each17.backend.service.impl;

import com.each17.backend.vocabulary.service.VocabularyServiceImpl;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LearningValuePolicy;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.each17.backend.song.repository.SongRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VocabularyServiceImplTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private ObjectMapper objectMapper;

    private VocabularyServiceImpl vocabularyService;

    @Mock
    private SongRepository songRepository;

    @Mock
    private LyricLineRepository lyricLineRepository;

    @Mock
    private LyricTokenRepository lyricTokenRepository;

    private EnglishLemmaService lemmaService;
    private LearningValuePolicy learningValuePolicy;
    private LyricTokenizationService tokenizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lemmaService = new EnglishLemmaService();
        learningValuePolicy = new LearningValuePolicy();
        tokenizationService = new LyricTokenizationService(lemmaService, learningValuePolicy);
        vocabularyService = new VocabularyServiceImpl(
                vocabularyRepository, songRepository, lyricLineRepository, lyricTokenRepository,
                tokenizationService, lemmaService, learningValuePolicy, objectMapper
        );
    }

    @Test
    void testGetWordListWithoutPrefix() {
        // Given
        int page = 0;
        int size = 2;  // 修改为实际数据大小
        Pageable pageable = PageRequest.of(page, size);
        
        Vocabulary vocab1 = Vocabulary.builder().word("love").build();
        Vocabulary vocab2 = Vocabulary.builder().word("yesterday").build();
        List<Vocabulary> vocabList = Arrays.asList(vocab1, vocab2);
        Page<Vocabulary> vocabPage = new PageImpl<>(vocabList);
        
        List<String> expectedWords = Arrays.asList("love", "yesterday");
        
        when(vocabularyRepository.findByRecommendedTrueOrderByWordAsc(pageable)).thenReturn(vocabPage);

        // When
        WordPageDto result = vocabularyService.getWordList(null, page, size);

        // Then
        assertEquals(expectedWords, result.getContent());
        assertEquals(vocabPage.getTotalElements(), result.getTotalElements());
        assertEquals(vocabPage.getTotalPages(), result.getTotalPages());
        assertEquals(page, result.getNumber());
        assertEquals(size, result.getSize());  // 修改断言
        
        verify(vocabularyRepository, times(1)).findByRecommendedTrueOrderByWordAsc(pageable);
    }

    @Test
    void testGetWordListWithPrefix() {
        // Given
        String prefix = "lov";
        int page = 0;
        int size = 2;  // 修改为实际数据大小
        Pageable pageable = PageRequest.of(page, size);
        
        Vocabulary vocab1 = Vocabulary.builder().word("love").build();
        Vocabulary vocab2 = Vocabulary.builder().word("lover").build();
        List<Vocabulary> vocabList = Arrays.asList(vocab1, vocab2);
        Page<Vocabulary> vocabPage = new PageImpl<>(vocabList);
        
        List<String> expectedWords = Arrays.asList("love", "lover");
        
        when(vocabularyRepository.findByRecommendedTrueAndWordStartingWithOrderByWordAsc(prefix.toLowerCase(), pageable)).thenReturn(vocabPage);

        // When
        WordPageDto result = vocabularyService.getWordList(prefix, page, size);

        // Then
        assertEquals(expectedWords, result.getContent());
        assertEquals(vocabPage.getTotalElements(), result.getTotalElements());
        assertEquals(vocabPage.getTotalPages(), result.getTotalPages());
        assertEquals(page, result.getNumber());
        assertEquals(size, result.getSize());  // 修改断言
        
        verify(vocabularyRepository, times(1)).findByRecommendedTrueAndWordStartingWithOrderByWordAsc(prefix.toLowerCase(), pageable);
    }

    @Test
    void testGetWordOccurrences() throws JsonProcessingException {
        // Given
        String word = "love";
        String occurrencesJson = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        
        Vocabulary vocabulary = Vocabulary.builder()
                .word(word)
                .occurrences(occurrencesJson)
                .build();
                
        WordOccurrenceDto occurrenceDto = WordOccurrenceDto.builder()
                .songTitle("Yesterday")
                .lyricLine("Yesterday, all my troubles seemed so far away")
                .build();
                
        List<WordOccurrenceDto> expectedOccurrences = Arrays.asList(occurrenceDto);
        
        when(vocabularyRepository.findById(word.toLowerCase())).thenReturn(Optional.of(vocabulary));
        when(objectMapper.readValue(eq(occurrencesJson), any(TypeReference.class))).thenReturn(expectedOccurrences);

        // When
        List<WordOccurrenceDto> result = vocabularyService.getWordOccurrences(word);

        // Then
        assertEquals(expectedOccurrences.size(), result.size());
        assertEquals(expectedOccurrences.get(0).getSongTitle(), result.get(0).getSongTitle());
        assertEquals(expectedOccurrences.get(0).getLyricLine(), result.get(0).getLyricLine());
        
        verify(vocabularyRepository, times(1)).findById(word.toLowerCase());
    }

    @Test
    void testGetWordOccurrencesNotFound() {
        // Given
        String word = "nonexistent";
        when(vocabularyRepository.findById(word.toLowerCase())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> vocabularyService.getWordOccurrences(word));
        verify(vocabularyRepository, times(1)).findById(word.toLowerCase());
    }

    @Test
    void testGetWordOccurrencesJsonProcessingException() throws JsonProcessingException {
        // Given
        String word = "love";
        String occurrencesJson = "invalid json";
        
        Vocabulary vocabulary = Vocabulary.builder()
                .word(word)
                .occurrences(occurrencesJson)
                .build();
        
        when(vocabularyRepository.findById(word.toLowerCase())).thenReturn(Optional.of(vocabulary));
        when(objectMapper.readValue(eq(occurrencesJson), any(TypeReference.class))).thenThrow(new JsonProcessingException("Invalid JSON") {});

        // When & Then
        assertThrows(RuntimeException.class, () -> vocabularyService.getWordOccurrences(word));
        verify(vocabularyRepository, times(1)).findById(word.toLowerCase());
    }
}
