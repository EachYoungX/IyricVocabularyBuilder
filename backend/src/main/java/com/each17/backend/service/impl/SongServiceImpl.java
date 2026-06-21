package com.each17.backend.service.impl;

import com.each17.backend.dto.*;
import com.each17.backend.entity.Song;
import com.each17.backend.mapper.SongMapper;
import com.each17.backend.repository.SongRepository;
import com.each17.backend.service.SongService;
import com.each17.backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final VocabularyService vocabularyService;

    // [核心] 使用 ConcurrentHashMap 在内存中存储任务状态，保证线程安全
    private final ConcurrentMap<UUID, ImportTaskResultDto> importTasks = new ConcurrentHashMap<>();

    // --- CRUD 方法 (保持不变) ---
    @Override
    public List<SongDto> getAllSongs() {
        return songRepository.findAll().stream().map(songMapper::toDto).collect(Collectors.toList());
    }


    @Override
    public SongImportResponseDto importSongsAsync(List<SongImportRequestDto> songsToImport) {
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
        importTasks.put(taskId, taskResult);

        log.info("[Task {}] Import task created for {} songs.", taskId, totalSongs);

        // 2. 调用异步方法来执行实际的耗时操作
        processSongImport(taskId, songsToImport);

        // 3. 立即返回任务信息给前端
        return SongImportResponseDto.builder()
                .taskId(taskId)
                .total(totalSongs)
                .message(totalSongs + " songs queued for import")
                .build();
    }

    @Async
    @Transactional
    @Override
    public void processSongImport(UUID taskId, List<SongImportRequestDto> songsToImport) {
        log.info("----------------------------------------------------");
        log.info("[Task {}] @Async method processSongImport has started in thread: {}", taskId, Thread.currentThread().getName());

        ImportTaskResultDto taskResult = importTasks.get(taskId);
        if (taskResult == null) {
            log.error("[Task {}] Task not found for processing.", taskId);
            return;
        }

        taskResult.setStatus("RUNNING");
        log.info("[Task {}] Now running in a background thread.", taskId);

        AtomicInteger currentIndex = new AtomicInteger(0);

        for (SongImportRequestDto songDto : songsToImport) {
            int index = currentIndex.getAndIncrement();
            try {
                if (songDto.getTitle() == null || songDto.getTitle().isBlank()) {
                    throw new IllegalArgumentException("Title cannot be empty");
                }

                Song song = songMapper.toEntity(songDto);

                // [核心修复] 在 save 之前，先检查是否存在
                // 注意：这在高并发下不是完美的，但对于我们的单用户应用足够了
                if (songRepository.existsByTitleAndArtist(song.getTitle(), song.getArtist())) {
                    throw new DataIntegrityViolationException("Song already exists (duplicate)");
                }

                songRepository.save(song); // 逐个保存，每个 save 都是一个独立的事务

                taskResult.setSuccessCount(taskResult.getSuccessCount() + 1);

            } catch (DataIntegrityViolationException e) {
                log.warn("[Task {}] Duplicate song at index {}: {}", taskId, index, songDto.getTitle());
                taskResult.setFailedCount(taskResult.getFailedCount() + 1);
                taskResult.getFailedItems().add(ImportTaskResultDto.FailedItemDto.builder()
                        .index(index).title(songDto.getTitle()).artist(songDto.getArtist())
                        .error("Song already exists (duplicate)").build());
            } catch (Exception e) {
                log.warn("[Task {}] Failed to import song at index {}: {}", taskId, index, e.getMessage());
                taskResult.setFailedCount(taskResult.getFailedCount() + 1);
                taskResult.getFailedItems().add(ImportTaskResultDto.FailedItemDto.builder()
                        .index(index).title(songDto.getTitle()).artist(songDto.getArtist())
                        .error(e.getMessage()).build());
            }
        }

        taskResult.setStatus("COMPLETED");
        taskResult.setFinishedAt(LocalDateTime.now());
        log.info("[Task {}] Import task completed. Success: {}, Failed: {}.", taskId, taskResult.getSuccessCount(), taskResult.getFailedCount());
        
        // 歌曲导入完成后自动刷新词汇索引
        if (taskResult.getSuccessCount() > 0) {
            log.info("[Task {}] Songs imported successfully, triggering vocabulary index refresh...", taskId);
            vocabularyService.refreshVocabularyIndexAsync();
        }
        
        log.info("----------------------------------------------------");
    }




    @Override
    public ImportTaskResultDto getImportTaskResult(UUID taskId) {
        // [核心重构] 从内存 Map 中直接返回任务的当前状态
        ImportTaskResultDto taskResult = importTasks.get(taskId);
        if (taskResult == null) {
            // 如果前端查询了一个不存在的 taskId，可以抛出异常或返回一个错误状态
            throw new RuntimeException("Task not found with id: " + taskId);
        }
        return taskResult;
    }

    @Override
    public SongDto getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));
        return songMapper.toDto(song);
    }
    
    @Override
    public SongDto createSong(SongImportRequestDto songDto) {
        // 校验歌曲数据
        if (songDto.getTitle() == null || songDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (songDto.getArtist() == null || songDto.getArtist().isBlank()) {
            throw new IllegalArgumentException("Artist cannot be empty");
        }
        if (songDto.getLyrics() == null || songDto.getLyrics().isBlank()) {
            throw new IllegalArgumentException("Lyrics cannot be empty");
        }
        
        Song song = songMapper.toEntity(songDto);
        Song savedSong = songRepository.save(song);
        
        // 创建歌曲后自动刷新词汇索引
        log.info("Song created, triggering vocabulary index refresh...");
        vocabularyService.refreshVocabularyIndexAsync();
        
        return songMapper.toDto(savedSong);
    }
    
    @Override
    public SongDto updateSong(Long id, SongUpdateRequestDto songDto) {
        Song existingSong = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));
        
        // 更新歌曲信息
        songMapper.updateEntityFromDto(songDto, existingSong);
        Song updatedSong = songRepository.save(existingSong);
        
        // 更新歌曲后自动刷新词汇索引
        log.info("Song updated, triggering vocabulary index refresh...");
        vocabularyService.refreshVocabularyIndexAsync();
        
        return songMapper.toDto(updatedSong);
    }
    
    @Override
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new RuntimeException("Song not found with id: " + id);
        }
        songRepository.deleteById(id);
        
        // 删除歌曲后自动刷新词汇索引
        log.info("Song deleted, triggering vocabulary index refresh...");
        vocabularyService.refreshVocabularyIndexAsync();
    }

    @Override
    public void deleteSongs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Song IDs cannot be null or empty");
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
            throw new RuntimeException("Songs not found with ids: " + notFoundIds);
        }

        // 批量删除歌曲
        songRepository.deleteAllById(ids);
        vocabularyService.refreshVocabularyIndexAsync();
    }
}