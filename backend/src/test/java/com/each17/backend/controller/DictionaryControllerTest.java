package com.each17.backend.controller;

import com.each17.backend.dictionary.controller.DictionaryController;
import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;
import com.each17.backend.dictionary.service.DictionaryService;
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

import java.util.List;

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
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.word").value("voyage"))
                .andExpect(jsonPath("$.data.phonetic").value("/ˈvɔɪ.ɪdʒ/"))
                .andExpect(jsonPath("$.data.definition").value("a long journey, especially by ship or in space"))
                .andExpect(jsonPath("$.data.translation").value("航行；旅行"));
    }

    @Test
    void testGetSourceInfo() throws Exception {
        when(dictionaryService.getSourceInfo()).thenReturn(List.of(
                DictionarySourceDto.builder()
                        .sourceName("ECDICT")
                        .sourceUrl("https://github.com/skywind3000/ECDICT")
                        .licenseName("MIT License")
                        .requiresAttribution(true)
                        .build(),
                DictionarySourceDto.builder()
                        .sourceName("2ndLA/english-phrases")
                        .sourceUrl("https://github.com/2ndLA/english-phrases")
                        .licenseName("CC BY-SA 4.0")
                        .requiresAttribution(true)
                        .build()));

        mockMvc.perform(get("/api/dictionary/source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceName").value("ECDICT"))
                .andExpect(jsonPath("$.data[0].sourceUrl").value("https://github.com/skywind3000/ECDICT"))
                .andExpect(jsonPath("$.data[0].licenseName").value("MIT License"))
                .andExpect(jsonPath("$.data[0].requiresAttribution").value(true))
                .andExpect(jsonPath("$.data[1].sourceName").value("2ndLA/english-phrases"))
                .andExpect(jsonPath("$.data[1].sourceUrl").value("https://github.com/2ndLA/english-phrases"))
                .andExpect(jsonPath("$.data[1].licenseName").value("CC BY-SA 4.0"));
    }
}
