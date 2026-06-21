package com.each17.backend.controller;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.service.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DictionaryController.class)
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DictionaryService dictionaryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLookupWord() throws Exception {
        // Given
        DictionaryEntryDto dictionaryEntryDto = DictionaryEntryDto.builder()
                .word("voyage")
                .phonetic("/ˈvɔɪ.ɪdʒ/")
                .definition("a long journey, especially by ship or in space")
                .translation("航行；旅行")
                .pos("noun, verb")
                .collins(5)
                .bnc(3025)
                .frq(2632)
                .forms("p:voyaged/d:voyaged/i:voyaging/s:voyages")
                .build();
        
        when(dictionaryService.lookupWord(anyString())).thenReturn(dictionaryEntryDto);

        // When & Then
        mockMvc.perform(get("/api/dictionary/{word}", "voyage"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.word").value("voyage"))
                .andExpect(jsonPath("$.phonetic").value("/ˈvɔɪ.ɪdʒ/"))
                .andExpect(jsonPath("$.definition").value("a long journey, especially by ship or in space"))
                .andExpect(jsonPath("$.translation").value("航行；旅行"));
    }
}