package com.each17.backend.lyric.controller;

import com.each17.backend.lyric.dto.*;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.service.LyricStructureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LyricController.class)
class LyricControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean LyricStructureService lyricStructureService;

    @Test
    void returnsStructuredLyricsInApiEnvelope() throws Exception {
        LyricLineDto line = new LyricLineDto(10L, 0, "[Verse]", "[Verse]", LyricLineType.SECTION_LABEL, true, 0.99, false);
        LyricDocumentDto document = new LyricDocumentDto(
                1L, "[Verse]", "[Verse]", "hash", 1, LocalDateTime.now().toString(), List.of(line)
        );
        when(lyricStructureService.getDocument(1L)).thenReturn(document);

        mockMvc.perform(get("/api/songs/1/lyrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.lines[0].lineType").value("SECTION_LABEL"))
                .andExpect(jsonPath("$.data.lines[0].hidden").value(true));
    }

    @Test
    void savesUserLineCorrection() throws Exception {
        LyricLineDto corrected = new LyricLineDto(10L, 0, "Taylor:", "Taylor:", LyricLineType.LYRIC, false, 1.0, true);
        when(lyricStructureService.updateLine(eq(1L), eq(10L), any())).thenReturn(corrected);

        mockMvc.perform(put("/api/songs/1/lyrics/lines/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineType\":\"LYRIC\",\"hidden\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineType").value("LYRIC"))
                .andExpect(jsonPath("$.data.userOverride").value(true));
    }
}
