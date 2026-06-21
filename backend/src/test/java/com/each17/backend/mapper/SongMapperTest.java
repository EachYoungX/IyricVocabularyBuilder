//package com.each17.backend.mapper;
//
//import com.each17.backend.dto.SongDto;
//import com.each17.backend.dto.SongImportRequestDto;
//import com.each17.backend.dto.SongUpdateRequestDto;
//import com.each17.backend.entity.Song;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//
//class SongMapperTest {
//
//    private final SongMapper songMapper = new SongMapper();
//
//    @Test
//    void testToDto() {
//        Song song = Song.builder()
//                .id(1L)
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        SongDto songDto = songMapper.toDto(song);
//
//        assertEquals(song.getId(), songDto.getId());
//        assertEquals(song.getTitle(), songDto.getTitle());
//        assertEquals(song.getArtist(), songDto.getArtist());
//        assertEquals(song.getLyrics(), songDto.getLyrics());
//    }
//
//    @Test
//    void testToEntityFromImportRequest() {
//        SongImportRequestDto dto = SongImportRequestDto.builder()
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        Song song = songMapper.toEntity(dto);
//
//        assertEquals(dto.getTitle(), song.getTitle());
//        assertEquals(dto.getArtist(), song.getArtist());
//        assertEquals(dto.getLyrics(), song.getLyrics());
//    }
//
//    @Test
//    void testToEntityFromUpdateRequest() {
//        SongUpdateRequestDto dto = SongUpdateRequestDto.builder()
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        Song song = songMapper.toEntity(dto);
//
//        assertEquals(dto.getTitle(), song.getTitle());
//        assertEquals(dto.getArtist(), song.getArtist());
//        assertEquals(dto.getLyrics(), song.getLyrics());
//    }
//
//    @Test
//    void testUpdateEntityFromDto() {
//        SongUpdateRequestDto dto = SongUpdateRequestDto.builder()
//                .title("Updated Title")
//                .artist("Updated Artist")
//                .lyrics("Updated lyrics")
//                .build();
//
//        Song song = new Song();
//        song.setTitle("Original Title");
//        song.setArtist("Original Artist");
//        song.setLyrics("Original lyrics");
//
//        songMapper.updateEntityFromDto(dto, song);
//
//        assertEquals(dto.getTitle(), song.getTitle());
//        assertEquals(dto.getArtist(), song.getArtist());
//        assertEquals(dto.getLyrics(), song.getLyrics());
//    }
//}