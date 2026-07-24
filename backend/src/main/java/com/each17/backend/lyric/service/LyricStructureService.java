package com.each17.backend.lyric.service;

import com.each17.backend.common.exception.ConflictException;
import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.lyric.dto.*;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LyricStructureService {
    private final SongRepository songRepository;
    private final LyricLineRepository lyricLineRepository;
    private final LyricNormalizer lyricNormalizer;
    private final LyricLineClassifier lyricLineClassifier;
    private final LyricsHashService lyricsHashService;
    private final VocabularyService vocabularyService;
    private final LyricTokenRepository lyricTokenRepository;

    public LyricDocumentDto getDocument(Long songId) {
        Song song = getSong(songId);
        if (!lyricLineRepository.existsBySongId(songId)) {
            structureSong(song, resolveRawLyrics(song), true);
        }
        return toDocument(song, lyricLineRepository.findBySongIdOrderByLineIndexAsc(songId));
    }

    public LyricDocumentDto importLyrics(Long songId, LyricImportRequestDto request) {
        Song song = getSong(songId);
        LyricDocumentDto result = structureSong(song, request.lyrics(), request.overwrite());
        vocabularyService.refreshVocabularyIndexAsync();
        return result;
    }

    public boolean isSameContent(Song song, String rawLyrics) {
        String candidateHash = lyricsHashService.hash(lyricNormalizer.normalize(rawLyrics).text());
        if (song.getLyricsHash() != null) return song.getLyricsHash().equals(candidateHash);
        return lyricsHashService.hash(lyricNormalizer.normalize(resolveRawLyrics(song)).text()).equals(candidateHash);
    }

    public LyricDocumentDto structureSong(Song song, String rawLyrics, boolean overwrite) {
        return structureSong(song, rawLyrics, overwrite, false);
    }

    public LyricDocumentDto structureSong(Song song, String rawLyrics, boolean overwrite, boolean preserveOriginalLyrics) {
        if (rawLyrics == null || rawLyrics.isBlank()) {
            throw new ValidationException("Lyrics cannot be empty");
        }

        LyricNormalizer.NormalizedLyrics normalized = lyricNormalizer.normalize(rawLyrics);
        String newHash = lyricsHashService.hash(normalized.text());
        String oldHash = song.getLyricsHash();
        boolean sameContent = newHash.equals(oldHash);

        if (sameContent && lyricLineRepository.existsBySongId(song.getId())) {
            return toDocument(song, lyricLineRepository.findBySongIdOrderByLineIndexAsc(song.getId()));
        }
        if (oldHash != null && !sameContent && !overwrite) {
            throw new ConflictException("Lyrics differ from the imported version; set overwrite=true to replace them");
        }

        Map<String, Deque<LyricLine>> overrides = lyricLineRepository.findBySongIdOrderByLineIndexAsc(song.getId()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getUserOverride()))
                .collect(Collectors.groupingBy(
                        LyricLine::getOriginalText,
                        Collectors.toCollection(ArrayDeque::new)
                ));

        lyricTokenRepository.deleteBySongId(song.getId());
        lyricLineRepository.deleteBySongId(song.getId());
        lyricLineRepository.flush();

        song.setLyrics(rawLyrics);
        if (!preserveOriginalLyrics || song.getRawLyrics() == null || song.getRawLyrics().isBlank()) {
            song.setRawLyrics(rawLyrics);
        }
        song.setNormalizedLyrics(normalized.text());
        song.setLyricsHash(newHash);
        if (oldHash != null && !sameContent) {
            song.setImportVersion(Math.max(1, Objects.requireNonNullElse(song.getImportVersion(), 1)) + 1);
        } else if (song.getImportVersion() == null) {
            song.setImportVersion(1);
        }
        song.setUpdatedAt(LocalDateTime.now().toString());
        Song savedSong = songRepository.save(song);

        List<LyricLine> lines = new ArrayList<>();
        for (int index = 0; index < normalized.lines().size(); index++) {
            LyricNormalizer.NormalizedLine normalizedLine = normalized.lines().get(index);
            LyricLineClassifier.Classification classification = lyricLineClassifier.classify(normalizedLine.normalizedText());
            LyricLine line = LyricLine.builder()
                    .song(savedSong)
                    .lineIndex(index)
                    .originalText(normalizedLine.originalText())
                    .normalizedText(normalizedLine.normalizedText())
                    .lineType(classification.lineType())
                    .hidden(classification.hidden())
                    .confidence(classification.confidence())
                    .userOverride(false)
                    .build();

            Deque<LyricLine> matchingOverrides = overrides.get(normalizedLine.originalText());
            if (matchingOverrides != null && !matchingOverrides.isEmpty()) {
                LyricLine override = matchingOverrides.removeFirst();
                line.setNormalizedText(override.getNormalizedText());
                line.setLineType(override.getLineType());
                line.setHidden(override.getHidden());
                line.setConfidence(1.0);
                line.setUserOverride(true);
            }
            lines.add(line);
        }

        return toDocument(savedSong, lyricLineRepository.saveAll(lines));
    }

    public LyricLineDto updateLine(Long songId, Long lineId, LyricLineUpdateRequestDto request) {
        LyricLine line = lyricLineRepository.findByIdAndSongId(lineId, songId)
                .orElseThrow(() -> new NotFoundException("Lyric line not found: " + lineId));
        if (request.normalizedText() != null) line.setNormalizedText(request.normalizedText().trim());
        if (request.lineType() != null) line.setLineType(request.lineType());
        if (request.hidden() != null) line.setHidden(request.hidden());
        line.setConfidence(1.0);
        line.setUserOverride(true);
        return toLineDto(lyricLineRepository.save(line));
    }

    public void deleteLinesForSong(Long songId) {
        lyricTokenRepository.deleteBySongId(songId);
        lyricLineRepository.deleteBySongId(songId);
    }

    private Song getSong(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new NotFoundException("Song not found with id: " + songId));
    }

    private String resolveRawLyrics(Song song) {
        return song.getRawLyrics() != null ? song.getRawLyrics() : song.getLyrics();
    }

    private LyricDocumentDto toDocument(Song song, List<LyricLine> lines) {
        return new LyricDocumentDto(
                song.getId(), resolveRawLyrics(song), song.getNormalizedLyrics(), song.getLyricsHash(),
                song.getImportVersion(), song.getUpdatedAt(), lines.stream().map(this::toLineDto).toList()
        );
    }

    private LyricLineDto toLineDto(LyricLine line) {
        return new LyricLineDto(
                line.getId(), line.getLineIndex(), line.getOriginalText(), line.getNormalizedText(),
                line.getLineType(), line.getHidden(), line.getConfidence(), line.getUserOverride()
        );
    }
}
