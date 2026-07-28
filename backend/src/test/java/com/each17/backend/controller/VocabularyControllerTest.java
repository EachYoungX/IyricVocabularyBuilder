package com.each17.backend.controller;

import com.each17.backend.vocabulary.controller.VocabularyController;
import com.each17.backend.dto.VocabularyBulkWordsRequestDto;
import com.each17.backend.dto.VocabularyLearningValueRequestDto;
import com.each17.backend.dto.VocabularyQualityCandidateDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.vocabulary.service.VocabularyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularyController.class)
class VocabularyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VocabularyService vocabularyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetWordList() throws Exception {
        // Given
        WordPageDto wordPageDto = WordPageDto.builder()
                .content(Arrays.asList("love", "yesterday"))
                .totalElements(2L)
                .totalPages(1)
                .number(0)
                .size(2)
                .build();
        
        when(vocabularyService.getWordList(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(wordPageDto);

        // When & Then
        mockMvc.perform(get("/api/vocabulary/words")
                .param("prefix", "lo")
                .param("page", "0")
                .param("size", "10")
                .param("recommendedOnly", "false")
                .param("lemmaSearch", "false")
                .param("includePhrases", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0]").value("love"))
                .andExpect(jsonPath("$.data.content[1]").value("yesterday"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void testGetWordOccurrences() throws Exception {
        // Given
        WordOccurrenceDto occurrence1 = WordOccurrenceDto.builder()
                .songTitle("Yesterday")
                .lyricLine("Yesterday, all my troubles seemed so far away")
                .build();
                
        WordOccurrenceDto occurrence2 = WordOccurrenceDto.builder()
                .songTitle("Love Me Do")
                .lyricLine("Love, love me do")
                .build();
                
        List<WordOccurrenceDto> occurrences = Arrays.asList(occurrence1, occurrence2);
        
        when(vocabularyService.getWordOccurrences(anyString())).thenReturn(occurrences);

        // When & Then
        mockMvc.perform(get("/api/vocabulary/words/{word}/occurrences", "love"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].songTitle").value("Yesterday"))
                .andExpect(jsonPath("$.data[1].songTitle").value("Love Me Do"));
    }

    @Test
    void testDeleteVocabularyWords() throws Exception {
        VocabularyBulkWordsRequestDto request = new VocabularyBulkWordsRequestDto();
        request.setWords(List.of("ain't leav", "all eye"));
        when(vocabularyService.deleteWords(List.of("ain't leav", "all eye"))).thenReturn(2);

        mockMvc.perform(delete("/api/vocabulary/words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void testUpdateVocabularyLearningValue() throws Exception {
        VocabularyLearningValueRequestDto request = new VocabularyLearningValueRequestDto();
        request.setRecommended(false);
        VocabularyQualityCandidateDto response = VocabularyQualityCandidateDto.builder()
                .word("yeah")
                .learningScore(0.25)
                .occurrenceCount(3)
                .songCount(1)
                .recommended(false)
                .reasons(List.of("LOW_LEARNING_VALUE"))
                .examples(List.of())
                .build();
        when(vocabularyService.updateLearningValue("yeah", false)).thenReturn(response);

        mockMvc.perform(patch("/api/vocabulary/words/{word}/learning-value", "yeah")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.word").value("yeah"))
                .andExpect(jsonPath("$.data.recommended").value(false))
                .andExpect(jsonPath("$.data.learningScore").value(0.25));
    }

    @Test
    void testRefreshVocabularyIndex() throws Exception {
        // Given
        UUID taskId = UUID.randomUUID();
        when(vocabularyService.refreshVocabularyIndexAsync()).thenReturn(taskId);

        // When & Then
        mockMvc.perform(post("/api/vocabulary/refresh"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.data.message").value("Vocabulary index rebuild task started"))
                .andExpect(jsonPath("$.data.taskId").value(taskId.toString()));
    }
}
