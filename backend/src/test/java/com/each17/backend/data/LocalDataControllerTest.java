package com.each17.backend.data;

import com.each17.backend.vocabulary.service.VocabularyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalDataController.class)
class LocalDataControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private LocalDataService localDataService;
    @MockitoBean
    private VocabularyService vocabularyService;

    @Test
    void clearsDataBeforeRequestingTheFinalEmptyRebuild() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(vocabularyService.refreshVocabularyIndexAsync()).thenReturn(taskId);

        mockMvc.perform(delete("/api/data/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vocabularyRebuildTaskId").value(taskId.toString()));

        var ordered = inOrder(localDataService, vocabularyService);
        ordered.verify(localDataService).clearAll();
        ordered.verify(vocabularyService).refreshVocabularyIndexAsync();
    }
}
