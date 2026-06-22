package com.each17.backend.song.controller;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongImportResponseDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.common.response.ApiResponse;
import com.each17.backend.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SongDto>>> getAllSongs() {
        return ResponseEntity.ok(ApiResponse.success(songService.getAllSongs()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SongDto>> getSongById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(songService.getSongById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SongDto>> createSong(@RequestBody SongImportRequestDto songDto) {
        return ResponseEntity.ok(ApiResponse.success(songService.createSong(songDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SongDto>> updateSong(@PathVariable Long id, @RequestBody SongUpdateRequestDto songDto) {
        return ResponseEntity.ok(ApiResponse.success(songService.updateSong(id, songDto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<SongImportResponseDto>> importSongs(@RequestBody List<SongImportRequestDto> songsToImport) {
        SongImportResponseDto response = songService.importSongsAsync(songsToImport);
        return ResponseEntity.accepted().body(ApiResponse.accepted(response));
    }
    
    @GetMapping("/import/tasks/{taskId}")
    public ResponseEntity<ApiResponse<ImportTaskResultDto>> getImportTaskResult(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(songService.getImportTaskResult(taskId)));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteSongs(@RequestBody List<Long> ids) {
        songService.deleteSongs(ids);
        return ResponseEntity.noContent().build();
    }
}   
