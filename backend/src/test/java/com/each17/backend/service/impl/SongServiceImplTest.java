package com.each17.backend.service.impl;

import com.each17.backend.song.service.SongServiceImpl;
import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.mapper.SongMapper;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.VocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SongServiceImplTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private SongMapper songMapper;

    @Mock
    private VocabularyService vocabularyService;

    @InjectMocks
    private SongServiceImpl songService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllSongs() {
        // Given
        Song song1 = new Song();
        Song song2 = new Song();
        List<Song> songs = Arrays.asList(song1, song2);

        SongDto songDto1 = new SongDto();
        SongDto songDto2 = new SongDto();
        List<SongDto> expectedDtos = Arrays.asList(songDto1, songDto2);

        when(songRepository.findAll()).thenReturn(songs);
        when(songMapper.toDto(song1)).thenReturn(songDto1);
        when(songMapper.toDto(song2)).thenReturn(songDto2);

        // When
        List<SongDto> result = songService.getAllSongs();

        // Then
        assertEquals(expectedDtos, result);
        verify(songRepository, times(1)).findAll();
        verify(songMapper, times(2)).toDto(any(Song.class));
    }

    @Test
    void testGetSongById() {
        // Given
        Long songId = 1L;
        Song song = new Song();
        SongDto expectedDto = new SongDto();

        when(songRepository.findById(songId)).thenReturn(Optional.of(song));
        when(songMapper.toDto(song)).thenReturn(expectedDto);

        // When
        SongDto result = songService.getSongById(songId);

        // Then
        assertEquals(expectedDto, result);
        verify(songRepository, times(1)).findById(songId);
        verify(songMapper, times(1)).toDto(song);
    }

    @Test
    void testGetSongByIdNotFound() {
        // Given
        Long songId = 1L;
        when(songRepository.findById(songId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> songService.getSongById(songId));
        verify(songRepository, times(1)).findById(songId);
    }

    @Test
    void testCreateSong() {
        // Given
        SongImportRequestDto requestDto = new SongImportRequestDto();
        requestDto.setTitle("Test Song");
        requestDto.setArtist("Test Artist");
        requestDto.setLyrics("Test lyrics content");
        
        Song song = new Song();
        Song savedSong = new Song();
        SongDto expectedDto = new SongDto();

        when(songMapper.toEntity(requestDto)).thenReturn(song);
        when(songRepository.save(song)).thenReturn(savedSong);
        when(songMapper.toDto(savedSong)).thenReturn(expectedDto);

        // When
        SongDto result = songService.createSong(requestDto);

        // Then
        assertEquals(expectedDto, result);
        verify(songMapper, times(1)).toEntity(requestDto);
        verify(songRepository, times(1)).save(song);
        verify(songMapper, times(1)).toDto(savedSong);
        verify(vocabularyService, times(1)).refreshVocabularyIndexAsync();
    }

    @Test
    void testUpdateSong() {
        // Given
        Long songId = 1L;
        SongUpdateRequestDto requestDto = new SongUpdateRequestDto();
        Song existingSong = new Song();
        Song updatedSong = new Song();
        SongDto expectedDto = new SongDto();

        when(songRepository.findById(songId)).thenReturn(Optional.of(existingSong));
        when(songRepository.save(existingSong)).thenReturn(updatedSong);
        when(songMapper.toDto(updatedSong)).thenReturn(expectedDto);

        // When
        SongDto result = songService.updateSong(songId, requestDto);

        // Then
        assertEquals(expectedDto, result);
        verify(songRepository, times(1)).findById(songId);
        verify(songRepository, times(1)).save(existingSong);
        verify(songMapper, times(1)).updateEntityFromDto(requestDto, existingSong);
        verify(songMapper, times(1)).toDto(updatedSong);
        verify(vocabularyService, times(1)).refreshVocabularyIndexAsync();
    }

    @Test
    void testUpdateSongNotFound() {
        // Given
        Long songId = 1L;
        SongUpdateRequestDto requestDto = new SongUpdateRequestDto();
        when(songRepository.findById(songId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> songService.updateSong(songId, requestDto));
        verify(songRepository, times(1)).findById(songId);
    }

    @Test
    void testDeleteSong() {
        // Given
        Long songId = 1L;

        when(songRepository.existsById(songId)).thenReturn(true);

        // When
        songService.deleteSong(songId);

        // Then
        verify(songRepository, times(1)).existsById(songId);
        verify(songRepository, times(1)).deleteById(songId);
        verify(vocabularyService, times(1)).refreshVocabularyIndexAsync();
    }

    @Test
    void testDeleteSongNotFound() {
        // Given
        Long songId = 1L;
        when(songRepository.existsById(songId)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> songService.deleteSong(songId));
        verify(songRepository, times(1)).existsById(songId);
        verify(songRepository, times(0)).deleteById(songId);
    }
}
