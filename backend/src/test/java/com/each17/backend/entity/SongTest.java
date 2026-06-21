//package com.each17.backend.entity;
//
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//
//class SongTest {
//
//    @Test
//    void testSongBuilder() {
//        Song song = Song.builder()
//                .id(1L)
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        assertEquals(1L, song.getId());
//        assertEquals("Yesterday", song.getTitle());
//        assertEquals("The Beatles", song.getArtist());
//        assertEquals("Yesterday, all my troubles seemed so far away", song.getLyrics());
//    }
//
//    @Test
//    void testSongSettersAndGetters() {
//        Song song = new Song();
//        song.setId(1L);
//        song.setTitle("Yesterday");
//        song.setArtist("The Beatles");
//        song.setLyrics("Yesterday, all my troubles seemed so far away");
//
//        assertEquals(1L, song.getId());
//        assertEquals("Yesterday", song.getTitle());
//        assertEquals("The Beatles", song.getArtist());
//        assertEquals("Yesterday, all my troubles seemed so far away", song.getLyrics());
//    }
//}