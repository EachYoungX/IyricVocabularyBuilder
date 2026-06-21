package com.each17.backend.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongImportResponseDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SongService {
    List<SongDto> getAllSongs();
    SongDto getSongById(Long id);
    SongDto createSong(SongImportRequestDto songDto);
    SongDto updateSong(Long id, SongUpdateRequestDto songDto);
    void deleteSong(Long id);
    void deleteSongs(List<Long> ids);
    SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport);

    @Async
    @Transactional
    void processSongImport(UUID taskId, List<SongImportRequestDto> songsToImport);

    ImportTaskResultDto getImportTaskResult(UUID taskId);
}