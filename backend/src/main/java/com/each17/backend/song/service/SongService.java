package com.each17.backend.song.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongImportResponseDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.dto.SongSummaryDto;

import java.util.List;
import java.util.UUID;

public interface SongService {
    List<SongSummaryDto> getAllSongs();
    long countSongs();
    SongDto getSongById(Long id);
    SongDto createSong(SongImportRequestDto songDto);
    SongDto updateSong(Long id, SongUpdateRequestDto songDto);
    void deleteSong(Long id);
    void deleteSongs(List<Long> ids);
    SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport);
    SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport, boolean autoAddToPersonalVocabulary);

    ImportTaskResultDto getImportTaskResult(UUID taskId);
}
