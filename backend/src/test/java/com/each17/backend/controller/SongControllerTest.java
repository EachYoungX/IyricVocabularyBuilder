package com.each17.backend.controller;

import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongImportResponseDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.service.SongService;
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

@WebMvcTest(SongController.class)
class SongControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SongService songService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllSongs() throws Exception {
        // Given
        SongDto songDto1 = SongDto.builder()
                .id(1L)
                .title("Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
                
        SongDto songDto2 = SongDto.builder()
                .id(2L)
                .title("Hey Jude")
                .artist("The Beatles")
                .lyrics("Hey Jude, don't make it bad")
                .build();
                
        List<SongDto> songs = Arrays.asList(songDto1, songDto2);
        
        when(songService.getAllSongs()).thenReturn(songs);

        // When & Then
        mockMvc.perform(get("/api/songs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Yesterday"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Hey Jude"));
    }

    @Test
    void testCreateSong() throws Exception {
        // Given
        SongImportRequestDto requestDto = SongImportRequestDto.builder()
                .title("Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
                
        SongDto responseDto = SongDto.builder()
                .id(1L)
                .title("Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
        
        when(songService.createSong(any(SongImportRequestDto.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Yesterday"));
    }

    @Test
    void testUpdateSong() throws Exception {
        // Given
        Long songId = 1L;
        SongUpdateRequestDto requestDto = SongUpdateRequestDto.builder()
                .title("Updated Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
                
        SongDto responseDto = SongDto.builder()
                .id(songId)
                .title("Updated Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
        
        when(songService.updateSong(eq(songId), any(SongUpdateRequestDto.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(put("/api/songs/{id}", songId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(songId))
                .andExpect(jsonPath("$.title").value("Updated Yesterday"));
    }

    @Test
    void testDeleteSong() throws Exception {
        // Given
        Long songId = 1L;
        
        // When & Then
        mockMvc.perform(delete("/api/songs/{id}", songId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testImportSongs() throws Exception {
        // Given
        SongImportRequestDto requestDto1 = SongImportRequestDto.builder()
                .title("Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();
                
        SongImportRequestDto requestDto2 = SongImportRequestDto.builder()
                .title("Hey Jude")
                .artist("The Beatles")
                .lyrics("Hey Jude, don't make it bad")
                .build();
                
        List<SongImportRequestDto> requestDtos = Arrays.asList(requestDto1, requestDto2);
        
        UUID taskId = UUID.randomUUID();
        SongImportResponseDto responseDto = SongImportResponseDto.builder()
                .taskId(taskId)
                .total(2)
                .message("2 songs queued for import")
                .build();
        when(songService.importSongsAsync(anyList())).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/songs/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDtos)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.message").value("2 songs queued for import"));
    }
}