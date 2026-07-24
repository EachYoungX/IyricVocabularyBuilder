package com.each17.backend.song.mapper;

import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.song.entity.Song;
import org.springframework.stereotype.Component;

@Component
public class SongMapper {
    public SongDto toDto(Song song) {
        return SongDto.builder()
            .id(Long.valueOf(song.getId()))
            .title(song.getTitle())
            .artist(song.getArtist())
            .lyrics(song.getLyrics())
            .build();
    }

    public Song toEntity(SongImportRequestDto dto) {
        return Song.builder()
            .title(dto.getTitle())
            .artist(dto.getArtist())
            .lyrics(dto.getLyrics())
            .rawLyrics(dto.getLyrics())
            .importVersion(1)
            .build();
    }
    
    public Song toEntity(SongUpdateRequestDto dto) {
        return Song.builder()
            .title(dto.getTitle())
            .artist(dto.getArtist())
            .lyrics(dto.getLyrics())
            .rawLyrics(dto.getLyrics())
            .build();
    }
    
    public void updateEntityFromDto(SongUpdateRequestDto dto, Song entity) {
        entity.setTitle(dto.getTitle());
        entity.setArtist(dto.getArtist());
        entity.setLyrics(dto.getLyrics());
    }
}
