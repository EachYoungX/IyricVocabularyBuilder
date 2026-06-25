package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.lyric.service.EnglishLemmaService;
import com.each17.backend.lyric.service.LearningValuePolicy;
import com.each17.backend.lyric.service.LyricTokenizationService;
import com.each17.backend.song.entity.Song;
import com.each17.backend.vocabulary.entity.Vocabulary;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.repository.VocabularyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final SongRepository songRepository;
    private final LyricLineRepository lyricLineRepository;
    private final LyricTokenRepository lyricTokenRepository;
    private final LyricTokenizationService tokenizationService;
    private final EnglishLemmaService lemmaService;
    private final LearningValuePolicy learningValuePolicy;
    private final ObjectMapper objectMapper;

    // 任务状态存储（内存，生产环境可以换成 Redis）
    private final ConcurrentMap<UUID, VocabularyRebuildTaskDto> rebuildTasks = new ConcurrentHashMap<>();
    // ---------- 对外接口 ----------
    @Override
    public WordPageDto getWordList(String prefix, int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ValidationException("Page must be >= 0 and size must be between 1 and 200");
        }
        // 创建按单词字母顺序排序的分页请求
        Pageable pageable = PageRequest.of(page, size);
        Page<Vocabulary> vocabularyPage;

        if (prefix != null && !prefix.isBlank()) {
            String lemmaPrefix = lemmaService.lemma(tokenizationService.normalize(prefix));
            vocabularyPage = vocabularyRepository.findByRecommendedTrueAndWordStartingWithOrderByWordAsc(lemmaPrefix, pageable);
        } else {
            vocabularyPage = vocabularyRepository.findByRecommendedTrueOrderByWordAsc(pageable);
        }

        // 直接从数据库获取已排序的单词列表
        List<String> words = vocabularyPage.getContent().stream()
                .map(Vocabulary::getWord)
                .toList();

        return WordPageDto.builder()
                .content(words)
                .totalElements(vocabularyPage.getTotalElements())
                .totalPages(vocabularyPage.getTotalPages())
                .number(vocabularyPage.getNumber())
                .size(vocabularyPage.getSize())
                .build();
    }

    @Override
    public List<WordOccurrenceDto> getWordOccurrences(String word) {
        String lemma = lemmaService.lemma(tokenizationService.normalize(word));
        Optional<Vocabulary> vocabOpt = vocabularyRepository.findById(lemma);
        if (vocabOpt.isEmpty()) {
            throw new NotFoundException("Word not found: " + word);
        }

        try {
            // [核心实现]
            return objectMapper.readValue(vocabOpt.get().getOccurrences(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize occurrences for lemma: {}", lemma, e);
            throw new RuntimeException("Failed to deserialize occurrences for word: " + word, e);
        }
    }

    @Override
    public UUID refreshVocabularyIndexAsync() {
        UUID taskId = UUID.randomUUID();
        VocabularyRebuildTaskDto task = VocabularyRebuildTaskDto.builder()
                .taskId(taskId)
                .status("PENDING")
                .startedAt(LocalDateTime.now())
                .build();
        rebuildTasks.put(taskId, task);
        rebuildVocabularyInBackground(taskId);
        return taskId;
    }

    @Override
    public VocabularyRebuildTaskDto getRefreshTaskStatus(UUID taskId) {
        VocabularyRebuildTaskDto task = rebuildTasks.get(taskId);
        if (task == null) {
            throw new NotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    @Async
    @Transactional
    public void rebuildVocabularyInBackground(UUID taskId) {
        VocabularyRebuildTaskDto task = rebuildTasks.get(taskId);
        if (task == null) return;

        task.setStatus("RUNNING");
        log.info("[Refresh Task {}] Rebuilding vocabulary index...", taskId);

        try {
            List<Song> allSongs = songRepository.findAll();
            Map<String, LemmaIndex> newIndex = buildInvertedIndex(allSongs);

            List<Vocabulary> entities = newIndex.entrySet().stream()
                    .map(e -> {
                        try {
                            LemmaIndex value = e.getValue();
                            return Vocabulary.builder()
                                    .word(e.getKey())
                                    .occurrences(objectMapper.writeValueAsString(value.occurrences()))
                                    .displayForms(objectMapper.writeValueAsString(value.displayForms()))
                                    .occurrenceCount(value.occurrences().size())
                                    .songCount(value.songIds().size())
                                    .learningScore(value.learningScore())
                                    .recommended(learningValuePolicy.recommended(value.learningScore()))
                                    .build();
                        } catch (JsonProcessingException ex) {
                            log.warn("Failed to serialize occurrences for word: {}", e.getKey());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 原子替换
            vocabularyRepository.deleteAllInBatch();
            if (!entities.isEmpty()) {
                vocabularyRepository.saveAll(entities);
            }

            task.setStatus("COMPLETED");
            log.info("[Refresh Task {}] Vocabulary rebuilt successfully. {} words.", taskId, entities.size());

        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            log.error("[Refresh Task {}] Rebuild failed.", taskId, e);
        } finally {
            task.setFinishedAt(LocalDateTime.now());
            // 可选：任务结束 30 分钟后自动清理内存
            // refreshTasks.remove(taskId);
        }
    }

    // ==================== 核心倒排索引构建（已去掉进度） ====================
    private Map<String, LemmaIndex> buildInvertedIndex(List<Song> songs) {
        Map<String, LemmaIndex> index = new HashMap<>();
        lyricTokenRepository.deleteAllInBatch();

        for (Song song : songs) {
            List<LyricLine> lines = lyricLineRepository.findBySongIdOrderByLineIndexAsc(song.getId());
            if (lines.isEmpty()) {
                lines = fallbackLines(song);
            }

            List<LyricToken> songTokens = lines.stream()
                    .filter(line -> line.getLineType() == LyricLineType.LYRIC || line.getLineType() == LyricLineType.UNKNOWN)
                    .flatMap(line -> tokenizationService.tokenize(line).stream())
                    .toList();
            List<LyricToken> persistentTokens = songTokens.stream()
                    .filter(token -> token.getLyricLine().getId() != null)
                    .toList();
            if (!persistentTokens.isEmpty()) {
                lyricTokenRepository.saveAll(persistentTokens);
            }

            for (LyricToken token : songTokens) {
                LyricLine line = token.getLyricLine();
                LemmaIndex lemmaIndex = index.computeIfAbsent(token.getLemma(), ignored -> new LemmaIndex());
                lemmaIndex.add(token, line, song);
            }
        }
        return index;
    }

    private List<LyricLine> fallbackLines(Song song) {
        String lyrics = song.getNormalizedLyrics() != null ? song.getNormalizedLyrics()
                : (song.getRawLyrics() != null ? song.getRawLyrics() : song.getLyrics());
        if (lyrics == null) return List.of();
        String[] splitLines = lyrics.split("\\R");
        List<LyricLine> lines = new ArrayList<>();
        for (int i = 0; i < splitLines.length; i++) {
            lines.add(LyricLine.builder()
                    .song(song)
                    .lineIndex(i)
                    .originalText(splitLines[i])
                    .normalizedText(splitLines[i])
                    .lineType(LyricLineType.LYRIC)
                    .hidden(false)
                    .confidence(0.5)
                    .userOverride(false)
                    .build());
        }
        return lines;
    }

    private static final class LemmaIndex {
        private final List<WordOccurrenceDto> occurrences = new ArrayList<>();
        private final Set<String> displayForms = new TreeSet<>();
        private final Set<Long> songIds = new HashSet<>();
        private double learningScore = 0.0;

        void add(LyricToken token, LyricLine line, Song song) {
            displayForms.add(token.getSurfaceForm().toLowerCase());
            songIds.add(song.getId());
            learningScore = Math.max(learningScore, token.getLearningScore());
            occurrences.add(WordOccurrenceDto.builder()
                    .songId(song.getId())
                    .songTitle(song.getTitle())
                    .lyricLineId(line.getId())
                    .lineIndex(line.getLineIndex())
                    .lyricLine(line.getNormalizedText())
                    .surfaceForm(token.getSurfaceForm())
                    .lemma(token.getLemma())
                    .startOffset(token.getStartOffset())
                    .endOffset(token.getEndOffset())
                    .learningScore(token.getLearningScore())
                    .build());
        }

        List<WordOccurrenceDto> occurrences() {
            return occurrences;
        }

        Set<String> displayForms() {
            return displayForms;
        }

        Set<Long> songIds() {
            return songIds;
        }

        double learningScore() {
            return learningScore;
        }
    }
}
