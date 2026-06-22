package com.each17.backend.vocabulary.service;

import com.each17.backend.dto.ImportTaskResultDto;
import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.VocabularyRebuildTaskDto;
import com.each17.backend.dto.WordOccurrenceDto;
import com.each17.backend.dto.WordPageDto;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final SongRepository songRepository;
    private final ObjectMapper objectMapper;

    // 任务状态存储（内存，生产环境可以换成 Redis）
    private final ConcurrentMap<UUID, VocabularyRebuildTaskDto> rebuildTasks = new ConcurrentHashMap<>();
    // ---------- 停用词 ----------
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "ain't", "as",
            "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't",
            "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down",
            "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't",
            "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself",
            "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
            "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of",
            "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
            "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than",
            "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these",
            "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under",
            "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't",
            "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom",
            "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've",
            "your", "yours", "yourself", "yourselves", "o", "oh", "ooh", "oooh", "ah", "ahh", "ahhh", "ahhhh", "yeah", "la", "na", "y'all", "ya", "yu",
            "cause", "em", "fore", "til", "ok"
    );

    // ---------- 分词正则 ----------
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\w']+");

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
            vocabularyPage = vocabularyRepository.findByWordStartingWithOrderByWordAsc(prefix.toLowerCase(), pageable);
        } else {
            vocabularyPage = vocabularyRepository.findAllByOrderByWordAsc(pageable);
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
        Optional<Vocabulary> vocabOpt = vocabularyRepository.findById(word.toLowerCase());
        if (vocabOpt.isEmpty()) {
            throw new NotFoundException("Word not found: " + word);
        }

        try {
            // [核心实现]
            return objectMapper.readValue(vocabOpt.get().getOccurrences(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize occurrences for word: {}", word, e);
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
            Map<String, List<WordOccurrenceDto>> newIndex = buildInvertedIndex(allSongs);

            List<Vocabulary> entities = newIndex.entrySet().stream()
                    .map(e -> {
                        try {
                            return new Vocabulary(e.getKey(), objectMapper.writeValueAsString(e.getValue()));
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
    private Map<String, List<WordOccurrenceDto>> buildInvertedIndex(List<Song> songs) {
        Map<String, List<WordOccurrenceDto>> index = new HashMap<>();

        for (Song song : songs) {
            if (song.getLyrics() == null) continue;

            Arrays.stream(song.getLyrics().split("\r?\n|\r"))
                    .forEach(line -> tokenizeLine(line).stream()
                            .map(this::cleanAndValidateWord)
                            .filter(word -> word != null && !STOP_WORDS.contains(word))
                            .forEach(word -> index
                                    .computeIfAbsent(word, k -> new ArrayList<>())
                                    .add(new WordOccurrenceDto(song.getTitle(), line.trim()))));
        }
        return index;
    }

    private List<String> tokenizeLine(String line) {
        return WORD_PATTERN.matcher(line).results()
                .map(m -> m.group())
                .toList();
    }

    private String cleanAndValidateWord(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = raw.toLowerCase()
                .strip()
                .replaceAll("^[\\'\"(\\[.,!?;:\\])]+|[\\'\"(\\[.,!?;:\\])]+$", "");

        if (cleaned.matches("\\d+") ||           // 纯数字
                cleaned.chars().noneMatch(Character::isLetter) ||  // 没有字母
                cleaned.isEmpty()) {
            return null;
        }
        return cleaned;
    }
}
