package com.each17.backend.controller;

import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.service.VocabularyService;
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
        
        when(vocabularyService.getWordList(anyString(), anyInt(), anyInt())).thenReturn(wordPageDto);

        // When & Then
        mockMvc.perform(get("/api/vocabulary/words")
                .param("prefix", "lo")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0]").value("love"))
                .andExpect(jsonPath("$.content[1]").value("yesterday"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
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
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].songTitle").value("Yesterday"))
                .andExpect(jsonPath("$[1].songTitle").value("Love Me Do"));
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
                .andExpect(jsonPath("$.message").value("Vocabulary index rebuild task started"))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()));
    }
}