package com.each17.backend.controller;

import com.each17.backend.dto.UserVocabularyDto;
import com.each17.backend.dto.UserVocabularyReviewItemDto;
import com.each17.backend.dto.UserVocabularyStatsDto;
import com.each17.backend.vocabulary.controller.UserVocabularyController;
import com.each17.backend.vocabulary.entity.VocabularyStatus;
import com.each17.backend.vocabulary.service.UserVocabularyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserVocabularyController.class)
class UserVocabularyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserVocabularyService userVocabularyService;

    @Test
    void addWordReturnsSavedVocabulary() throws Exception {
        when(userVocabularyService.addWord(any())).thenReturn(word("run", VocabularyStatus.NEW));

        mockMvc.perform(post("/api/user-vocabulary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lemma\":\"running\",\"note\":\"chorus\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lemma").value("run"))
                .andExpect(jsonPath("$.data.status").value("NEW"));
    }

    @Test
    void listWordsSupportsStatusFilter() throws Exception {
        when(userVocabularyService.listWords(eq(VocabularyStatus.LEARNING))).thenReturn(List.of(word("run", VocabularyStatus.LEARNING)));

        mockMvc.perform(get("/api/user-vocabulary").param("status", "LEARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lemma").value("run"))
                .andExpect(jsonPath("$.data[0].status").value("LEARNING"));
    }

    @Test
    void getStatsReturnsCounts() throws Exception {
        when(userVocabularyService.getStats()).thenReturn(UserVocabularyStatsDto.builder()
                .totalCount(2L)
                .newCount(1L)
                .learningCount(1L)
                .familiarCount(0L)
                .masteredCount(0L)
                .ignoredCount(0L)
                .dueReviewCount(1L)
                .recentWords(List.of(word("run", VocabularyStatus.NEW)))
                .build());

        mockMvc.perform(get("/api/user-vocabulary/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.dueReviewCount").value(1));
    }

    @Test
    void getReviewQueueReturnsItems() throws Exception {
        when(userVocabularyService.getReviewQueue(5)).thenReturn(List.of(UserVocabularyReviewItemDto.builder()
                .id(1L)
                .lemma("run")
                .status(VocabularyStatus.NEW)
                .masteryScore(0.0)
                .build()));

        mockMvc.perform(get("/api/user-vocabulary/review").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lemma").value("run"));
    }

    private UserVocabularyDto word(String lemma, VocabularyStatus status) {
        return UserVocabularyDto.builder()
                .id(1L)
                .userId("local")
                .lemma(lemma)
                .status(status)
                .masteryScore(0.0)
                .firstSeenAt("2026-06-30T10:00:00")
                .lastSeenAt("2026-06-30T10:00:00")
                .reviewDueAt("2026-06-30T10:00:00")
                .build();
    }
}
