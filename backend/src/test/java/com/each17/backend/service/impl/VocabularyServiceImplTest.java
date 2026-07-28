package com.each17.backend.service.impl;

import com.each17.backend.vocabulary.service.VocabularyServiceImpl;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.vocabulary.service.VocabularyIndexBuilder;
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
    private VocabularyIndexBuilder vocabularyIndexBuilder;

    private EnglishLemmaService lemmaService;
    private LyricTokenizationService tokenizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lemmaService = new EnglishLemmaService();
        tokenizationService = mock(LyricTokenizationService.class);
        when(tokenizationService.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toLowerCase());
        when(tokenizationService.normalizeToLemmaPhrase(anyString())).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0, String.class);
            return Arrays.stream(raw.toLowerCase().split("\\s+"))
                    .map(lemmaService::lemma)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
        });
        vocabularyService = new VocabularyServiceImpl(
                vocabularyRepository, songRepository, tokenizationService, lemmaService,
                vocabularyIndexBuilder, objectMapper
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
        WordPageDto result = vocabularyService.getWordList(null, page, size, true, true, true);

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
        WordPageDto result = vocabularyService.getWordList(prefix, page, size, true, true, true);

        // Then
        assertEquals(expectedWords, result.getContent());
        assertEquals(vocabPage.getTotalElements(), result.getTotalElements());
        assertEquals(vocabPage.getTotalPages(), result.getTotalPages());
        assertEquals(page, result.getNumber());
        assertEquals(size, result.getSize());  // 修改断言
        
        verify(vocabularyRepository, times(1)).findByRecommendedTrueAndWordStartingWithOrderByWordAsc(prefix.toLowerCase(), pageable);
    }

    @Test
    void testGetWordListCanIncludeLowValueWords() {
        int page = 0;
        int size = 2;
        Pageable pageable = PageRequest.of(page, size);
        Vocabulary vocab1 = Vocabulary.builder().word("ah").recommended(false).build();
        Vocabulary vocab2 = Vocabulary.builder().word("love").recommended(true).build();
        Page<Vocabulary> vocabPage = new PageImpl<>(List.of(vocab1, vocab2));

        when(vocabularyRepository.findAllByOrderByWordAsc(pageable)).thenReturn(vocabPage);

        WordPageDto result = vocabularyService.getWordList(null, page, size, false, true, true);

        assertEquals(List.of("ah", "love"), result.getContent());
        verify(vocabularyRepository).findAllByOrderByWordAsc(pageable);
    }

    @Test
    void testGetWordListCanUseExactPrefixSearch() {
        String prefix = "running";
        int page = 0;
        int size = 2;
        Pageable pageable = PageRequest.of(page, size);
        Page<Vocabulary> vocabPage = new PageImpl<>(List.of(Vocabulary.builder().word("running").build()));

        when(vocabularyRepository.findByWordStartingWithOrderByWordAsc(prefix, pageable)).thenReturn(vocabPage);

        WordPageDto result = vocabularyService.getWordList(prefix, page, size, false, false, true);

        assertEquals(List.of("running"), result.getContent());
        verify(vocabularyRepository).findByWordStartingWithOrderByWordAsc(prefix, pageable);
        verify(vocabularyRepository, never()).findByRecommendedTrueAndWordStartingWithOrderByWordAsc(anyString(), any());
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
        assertEquals(1.0, result.get(0).getLearningScore());
        
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

    @Test
    void testGetQualityCandidatesExplainsLowValueAndSuspiciousPhrases() throws JsonProcessingException {
        Vocabulary lowValue = Vocabulary.builder()
                .word("yeah")
                .occurrences("[]")
                .occurrenceCount(3)
                .songCount(1)
                .learningScore(0.25)
                .recommended(false)
                .build();
        Vocabulary phrase = Vocabulary.builder()
                .word("ain't leav")
                .occurrences("[]")
                .occurrenceCount(1)
                .songCount(1)
                .learningScore(1.0)
                .recommended(true)
                .build();

        when(vocabularyRepository.findByRecommendedFalseOrderByLearningScoreAscWordAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(lowValue));
        when(vocabularyRepository.findByWordContainingOrderByWordAsc(eq(" "), any(Pageable.class)))
                .thenReturn(List.of(phrase));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());

        var result = vocabularyService.getQualityCandidates(10);

        assertEquals(2, result.size());
        assertEquals("yeah", result.get(0).getWord());
        assertTrue(result.get(0).getReasons().contains("LOW_LEARNING_VALUE"));
        assertEquals("ain't leav", result.get(1).getWord());
        assertTrue(result.get(1).getReasons().contains("CONTRACTION_PHRASE"));
        assertTrue(result.get(1).getReasons().contains("POSSIBLE_TRUNCATED_LEMMA"));
    }

    @Test
    void testDeleteWordsNormalizesAndDeletesExistingWords() {
        Vocabulary love = Vocabulary.builder().word("love").build();
        Vocabulary runAway = Vocabulary.builder().word("run away").build();

        when(vocabularyRepository.findAllById(List.of("love", "run away")))
                .thenReturn(List.of(love, runAway));

        int result = vocabularyService.deleteWords(List.of("Love", "running away", "love"));

        assertEquals(2, result);
        verify(vocabularyRepository).findAllById(List.of("love", "run away"));
        verify(vocabularyRepository).deleteAllInBatch(List.of(love, runAway));
    }

    @Test
    void testUpdateLearningValueCanMarkWordLowValue() {
        Vocabulary vocabulary = Vocabulary.builder()
                .word("yeah")
                .occurrences("[]")
                .occurrenceCount(4)
                .songCount(2)
                .learningScore(1.0)
                .recommended(true)
                .build();
        when(vocabularyRepository.findById("yeah")).thenReturn(Optional.of(vocabulary));
        when(vocabularyRepository.save(vocabulary)).thenReturn(vocabulary);

        var result = vocabularyService.updateLearningValue("Yeah", false);

        assertFalse(vocabulary.getRecommended());
        assertEquals(0.25, vocabulary.getLearningScore());
        assertEquals("yeah", result.getWord());
        assertTrue(result.getReasons().contains("LOW_LEARNING_VALUE"));
    }

    @Test
    void testUpdateLearningValueCanMarkWordRecommended() {
        Vocabulary vocabulary = Vocabulary.builder()
                .word("love")
                .occurrences("[]")
                .occurrenceCount(4)
                .songCount(2)
                .learningScore(0.25)
                .recommended(false)
                .build();
        when(vocabularyRepository.findById("love")).thenReturn(Optional.of(vocabulary));
        when(vocabularyRepository.save(vocabulary)).thenReturn(vocabulary);

        var result = vocabularyService.updateLearningValue("love", true);

        assertTrue(vocabulary.getRecommended());
        assertEquals(1.0, vocabulary.getLearningScore());
        assertEquals("love", result.getWord());
        assertFalse(result.getReasons().contains("LOW_LEARNING_VALUE"));
    }
}
