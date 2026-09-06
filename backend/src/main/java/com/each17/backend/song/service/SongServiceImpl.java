package com.each17.backend.song.service;

import com.each17.backend.dto.*;
import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.mapper.SongMapper;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.VocabularyService;
import com.each17.backend.lyric.service.LyricStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final VocabularyService vocabularyService;
    private final LyricStructureService lyricStructureService;
    private final SongImportTaskRegistry importTaskRegistry;
    private final SongImportWorker importWorker;

    // --- CRUD 方法 (保持不变) ---
    @Override
    public List<SongSummaryDto> getAllSongs() {
        return songRepository.findAll().stream().map(songMapper::toSummaryDto).collect(Collectors.toList());
    }

    @Override
    public long countSongs() {
        return songRepository.count();
    }


    @Override
    public SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport) {
        return importSongsAsync(songsToImport, false);
    }

    @Override
    public SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport, boolean autoAddToPersonalVocabulary) {
        final UUID taskId = UUID.randomUUID();
        final int totalSongs = songsToImport.size();

        // 1. 立即创建并存储初始任务状态
        ImportTaskResultDto taskResult = ImportTaskResultDto.builder()
                .taskId(taskId)
                .status("PENDING")
                .total(totalSongs)
                .successCount(0)
                .failedCount(0)
                .startedAt(LocalDateTime.now())
                .failedItems(new ArrayList<>())
                .build();
        importTaskRegistry.put(taskResult);

        log.info("[Task {}] Import task created for {} songs.", taskId, totalSongs);

        // 2. 调用异步方法来执行实际的耗时操作
        importWorker.process(taskId, List.copyOf(songsToImport), autoAddToPersonalVocabulary);

        // 3. 立即返回任务信息给前端
        return SongImportResponseDto.builder()
                .taskId(taskId)
                .total(totalSongs)
                .message(totalSongs + " songs queued for import")
                .build();
    }

    @Override
    public ImportTaskResultDto getImportTaskResult(UUID taskId) {
        // [核心重构] 从内存 Map 中直接返回任务的当前状态
        ImportTaskResultDto taskResult = importTaskRegistry.get(taskId);
        if (taskResult == null) {
            // 如果前端查询了一个不存在的 taskId，可以抛出异常或返回一个错误状态
            throw new NotFoundException("Task not found with id: " + taskId);
        }
        return taskResult;
    }

    @Override
    public SongDto getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Song not found with id: " + id));
        return songMapper.toDto(song);
    }
    
    @Override
    @Transactional
    public SongDto createSong(SongImportRequestDto songDto) {
        validateSong(songDto.getTitle(), songDto.getArtist(), songDto.getLyrics());
        
        Song song = songMapper.toEntity(songDto);
        Song savedSong = songRepository.save(song);
        lyricStructureService.structureSong(savedSong, resolveSourceContent(songDto), true);
        
        // 创建歌曲后自动刷新词汇索引
        log.info("Song created, triggering vocabulary index refresh...");
        vocabularyService.requestVocabularyIndexRefreshAfterCommit();
        
        return songMapper.toDto(savedSong);
    }

    private String resolveSourceContent(SongImportRequestDto request) {
        return request.getRawSourceContent() != null && !request.getRawSourceContent().isBlank()
                ? request.getRawSourceContent() : request.getLyrics();
    }
    
    @Override
    @Transactional
    public SongDto updateSong(Long id, SongUpdateRequestDto songDto) {
        validateSong(songDto.getTitle(), songDto.getArtist(), songDto.getLyrics());
        Song existingSong = songRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Song not found with id: " + id));
        
        // 更新歌曲信息
        songMapper.updateEntityFromDto(songDto, existingSong);
        Song updatedSong = songRepository.save(existingSong);
        lyricStructureService.structureSong(updatedSong, songDto.getLyrics(), true, true);
        
        // 更新歌曲后自动刷新词汇索引
        log.info("Song updated, triggering vocabulary index refresh...");
        vocabularyService.requestVocabularyIndexRefreshAfterCommit();
        
        return songMapper.toDto(updatedSong);
    }
    
    @Override
    @Transactional
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new NotFoundException("Song not found with id: " + id);
        }
        lyricStructureService.deleteLinesForSong(id);
        songRepository.deleteById(id);
        
        // 删除歌曲后自动刷新词汇索引
        log.info("Song deleted, triggering vocabulary index refresh...");
        vocabularyService.requestVocabularyIndexRefreshAfterCommit();
    }

    @Override
    @Transactional
    public void deleteSongs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ValidationException("Song IDs cannot be null or empty");
        }

        // 检查所有歌曲是否存在
        List<Long> existingIds = songRepository.findAllById(ids)
                .stream()
                .map(Song::getId)
                .toList();

        List<Long> notFoundIds = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (!notFoundIds.isEmpty()) {
            throw new NotFoundException("Songs not found with ids: " + notFoundIds);
        }

        // 批量删除歌曲
        ids.forEach(lyricStructureService::deleteLinesForSong);
        songRepository.deleteAllById(ids);
        vocabularyService.requestVocabularyIndexRefreshAfterCommit();
    }

    private void validateSong(String title, String artist, String lyrics) {
        if (title == null || title.isBlank()) throw new ValidationException("Title cannot be empty");
        if (artist == null || artist.isBlank()) throw new ValidationException("Artist cannot be empty");
        if (lyrics == null || lyrics.isBlank()) throw new ValidationException("Lyrics cannot be empty");
    }
}
