package com.each17.backend.controller;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.dto.SongDto;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.dto.SongImportResponseDto;
import com.each17.backend.dto.SongUpdateRequestDto;
import com.each17.backend.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:9000")
public class SongController {

    private final SongService songService;

    @GetMapping
    public ResponseEntity<List<SongDto>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getSongById(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @PostMapping
    public ResponseEntity<SongDto> createSong(@RequestBody SongImportRequestDto songDto) {
        return ResponseEntity.ok(songService.createSong(songDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SongDto> updateSong(@PathVariable Long id, @RequestBody SongUpdateRequestDto songDto) {
        return ResponseEntity.ok(songService.updateSong(id, songDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/import")
    public ResponseEntity<SongImportResponseDto> importSongs(@RequestBody List<SongImportRequestDto> songsToImport) {
        SongImportResponseDto response = songService.importSongsAsync(songsToImport);
        return ResponseEntity.accepted().body(response);
    }
    
    @GetMapping("/import/tasks/{taskId}")
    public ResponseEntity<ImportTaskResultDto> getImportTaskResult(@PathVariable UUID taskId) {
        return ResponseEntity.ok(songService.getImportTaskResult(taskId));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteSongs(@RequestBody List<Long> ids) {
        songService.deleteSongs(ids);
        return ResponseEntity.noContent().build();
    }
}   