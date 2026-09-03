package com.each17.backend.song.mapper;

import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.service.SongCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SongMapper {
    private final SongCreditService songCreditService;

    public SongDto toDto(Song song) {
        return SongDto.builder()
            .id(Long.valueOf(song.getId()))
            .title(song.getTitle())
            .artist(song.getArtist())
            .album(song.getAlbum())
            .rawTitle(song.getRawTitle())
            .rawArtist(song.getRawArtist())
            .rawSourceContent(song.getRawSourceContent())
            .lyrics(song.getLyrics())
            .credits(song.getId() == null ? java.util.List.of() : songCreditService.findDtos(song.getId()))
            .build();
    }

    public Song toEntity(SongImportRequestDto dto) {
        return Song.builder()
            .title(dto.getTitle())
            .artist(dto.getArtist())
            .album(dto.getAlbum())
            .rawTitle(dto.getTitle())
            .rawArtist(dto.getArtist())
            .lyrics(dto.getLyrics())
            .rawLyrics(dto.getLyrics())
            .rawSourceContent(dto.getRawSourceContent() != null && !dto.getRawSourceContent().isBlank()
                ? dto.getRawSourceContent() : dto.getLyrics())
            .importVersion(1)
            .build();
    }
    
    public Song toEntity(SongUpdateRequestDto dto) {
        return Song.builder()
            .title(dto.getTitle())
            .artist(dto.getArtist())
            .album(dto.getAlbum())
            .lyrics(dto.getLyrics())
            .build();
    }
    
    public void updateEntityFromDto(SongUpdateRequestDto dto, Song entity) {
        entity.setTitle(dto.getTitle());
        entity.setArtist(dto.getArtist());
        entity.setAlbum(dto.getAlbum());
        entity.setLyrics(dto.getLyrics());
    }
}
