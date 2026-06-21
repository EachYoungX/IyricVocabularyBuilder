package com.each17.backend.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SongDtoTest {

    @Test
void testSongDtoBuilder() {
        SongDto songDto = SongDto.builder()
                .id(1L)
                .title("Yesterday")
                .artist("The Beatles")
                .lyrics("Yesterday, all my troubles seemed so far away")
                .build();

        assertEquals(1L, songDto.getId());
        assertEquals("Yesterday", songDto.getTitle());
        assertEquals("The Beatles", songDto.getArtist());
        assertEquals("Yesterday, all my troubles seemed so far away", songDto.getLyrics());
    }

    @Test
    void testSongDtoSettersAndGetters() {
        SongDto songDto = new SongDto();
        songDto.setId(1L);
        songDto.setTitle("Yesterday");
        songDto.setArtist("The Beatles");
        songDto.setLyrics("Yesterday, all my troubles seemed so far away");

        assertEquals(1L, songDto.getId());
        assertEquals("Yesterday", songDto.getTitle());
        assertEquals("The Beatles", songDto.getArtist());
        assertEquals("Yesterday, all my troubles seemed so far away", songDto.getLyrics());
    }
}