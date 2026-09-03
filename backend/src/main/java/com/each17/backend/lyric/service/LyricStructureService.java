package com.each17.backend.lyric.service;

import com.each17.backend.common.exception.ConflictException;
import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.lyric.dto.*;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricClassificationSource;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.song.service.SongCreditService;
import com.each17.backend.vocabulary.service.VocabularyService;
import com.each17.backend.vocabulary.service.PhraseOccurrenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LyricStructureService {
    private final SongRepository songRepository;
    private final LyricLineRepository lyricLineRepository;
    private final LyricNormalizer lyricNormalizer;
    private final LyricLineClassifier lyricLineClassifier;
    private final LyricsHashService lyricsHashService;
    private final VocabularyService vocabularyService;
    private final LyricTokenRepository lyricTokenRepository;
    private final LyricTokenizationService tokenizationService;
    private final PhraseOccurrenceService phraseOccurrenceService;
    private final SongCreditService songCreditService;

    public LyricStructureService(
            SongRepository songRepository,
            LyricLineRepository lyricLineRepository,
            LyricNormalizer lyricNormalizer,
            LyricLineClassifier lyricLineClassifier,
            LyricsHashService lyricsHashService,
            VocabularyService vocabularyService,
            LyricTokenRepository lyricTokenRepository
    ) {
        this(songRepository, lyricLineRepository, lyricNormalizer, lyricLineClassifier, lyricsHashService,
                vocabularyService, lyricTokenRepository, null, null, null);
    }

    @Autowired
    public LyricStructureService(
            SongRepository songRepository,
            LyricLineRepository lyricLineRepository,
            LyricNormalizer lyricNormalizer,
            LyricLineClassifier lyricLineClassifier,
            LyricsHashService lyricsHashService,
            VocabularyService vocabularyService,
            LyricTokenRepository lyricTokenRepository,
            LyricTokenizationService tokenizationService,
            PhraseOccurrenceService phraseOccurrenceService,
            SongCreditService songCreditService
    ) {
        this.songRepository = songRepository;
        this.lyricLineRepository = lyricLineRepository;
        this.lyricNormalizer = lyricNormalizer;
        this.lyricLineClassifier = lyricLineClassifier;
        this.lyricsHashService = lyricsHashService;
        this.vocabularyService = vocabularyService;
        this.lyricTokenRepository = lyricTokenRepository;
        this.tokenizationService = tokenizationService;
        this.phraseOccurrenceService = phraseOccurrenceService;
        this.songCreditService = songCreditService;
    }

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
        return structureSong(song, rawLyrics, overwrite, false, false);
    }

    public LyricDocumentDto structureSong(Song song, String rawLyrics, boolean overwrite, boolean preserveOriginalLyrics) {
        return structureSong(song, rawLyrics, overwrite, preserveOriginalLyrics, false);
    }

    public LyricDocumentDto structureSong(Song song, String rawLyrics, boolean overwrite,
                                          boolean preserveOriginalLyrics, boolean forceReparse) {
        if (rawLyrics == null || rawLyrics.isBlank()) {
            throw new ValidationException("Lyrics cannot be empty");
        }

        LyricNormalizer.NormalizedLyrics normalized = lyricNormalizer.normalize(rawLyrics);
        String newHash = lyricsHashService.hash(normalized.text());
        String oldHash = song.getLyricsHash();
        boolean sameContent = newHash.equals(oldHash);

        if (sameContent && lyricLineRepository.existsBySongId(song.getId()) && !forceReparse) {
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

        if (songCreditService != null) songCreditService.deleteForSong(song.getId());
        lyricTokenRepository.deleteBySongId(song.getId());
        lyricLineRepository.deleteBySongId(song.getId());
        lyricLineRepository.flush();

        String title = firstNonBlank(normalized.metadata().get("ti"), song.getTitle());
        String artist = firstNonBlank(normalized.metadata().get("ar"), song.getArtist());
        String album = firstNonBlank(normalized.metadata().get("al"), song.getAlbum());
        if (song.getRawTitle() == null || song.getRawTitle().isBlank()) song.setRawTitle(song.getTitle());
        if (song.getRawArtist() == null || song.getRawArtist().isBlank()) song.setRawArtist(song.getArtist());
        song.setTitle(title);
        song.setArtist(artist);
        song.setAlbum(album);
        if (!preserveOriginalLyrics || song.getRawLyrics() == null || song.getRawLyrics().isBlank()) {
            song.setRawLyrics(rawLyrics);
        }
        if (!preserveOriginalLyrics || song.getRawSourceContent() == null || song.getRawSourceContent().isBlank()) {
            song.setRawSourceContent(rawLyrics);
        }
        List<LyricLineClassifier.Classification> classifications = lyricLineClassifier.classifyLines(
                normalized.lines(), title, artist);
        String learningLyrics = buildLearningLyrics(normalized.lines(), classifications);
        song.setLyrics(learningLyrics);
        song.setNormalizedLyrics(learningLyrics);
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
            LyricLineClassifier.Classification classification = classifications.get(index);
            LyricLine line = LyricLine.builder()
                    .song(savedSong)
                    .lineIndex(index)
                    .originalText(normalizedLine.originalText())
                    .normalizedText(normalizedLine.normalizedText())
                    .lineType(classification.lineType())
                    .classificationSource(classification.source())
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
                line.setClassificationSource(LyricClassificationSource.MANUAL);
                line.setUserOverride(true);
            }
            lines.add(line);
        }

        List<LyricLine> savedLines = lyricLineRepository.saveAll(lines);
        if (songCreditService != null) songCreditService.replaceCredits(savedSong, savedLines);
        rebuildSongTokens(savedSong, savedLines);
        return toDocument(savedSong, savedLines);
    }

    public LyricLineDto updateLine(Long songId, Long lineId, LyricLineUpdateRequestDto request) {
        LyricLine line = lyricLineRepository.findByIdAndSongId(lineId, songId)
                .orElseThrow(() -> new NotFoundException("Lyric line not found: " + lineId));
        if (request.normalizedText() != null) line.setNormalizedText(request.normalizedText().trim());
        if (request.lineType() != null) line.setLineType(request.lineType());
        if (request.hidden() != null) line.setHidden(request.hidden());
        line.setConfidence(1.0);
        line.setUserOverride(true);
        line.setClassificationSource(LyricClassificationSource.MANUAL);
        LyricLine savedLine = lyricLineRepository.save(line);
        if (tokenizationService != null) {
            lyricTokenRepository.deleteByLyricLineId(savedLine.getId());
            if (isTokenizable(savedLine)) lyricTokenRepository.saveAll(tokenizationService.tokenize(savedLine));
            if (phraseOccurrenceService != null) phraseOccurrenceService.invalidateSong(songId);
        }
        return toLineDto(savedLine);
    }

    public void deleteLinesForSong(Long songId) {
        if (songCreditService != null) songCreditService.deleteForSong(songId);
        if (phraseOccurrenceService != null) phraseOccurrenceService.invalidateSong(songId);
        lyricTokenRepository.deleteBySongId(songId);
        lyricLineRepository.deleteBySongId(songId);
    }

    public void rebuildTokensForSong(Long songId) {
        Song song = getSong(songId);
        List<LyricLine> lines = lyricLineRepository.findBySongIdOrderByLineIndexAsc(songId);
        if (lines.isEmpty()) {
            structureSong(song, resolveRawLyrics(song), true);
            return;
        }
        rebuildSongTokens(song, lines);
    }

    public void reclassifySong(Long songId) {
        Song song = getSong(songId);
        structureSong(song, resolveRawSource(song), true, true, true);
    }

    private Song getSong(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new NotFoundException("Song not found with id: " + songId));
    }

    private String resolveRawLyrics(Song song) {
        return resolveRawSource(song);
    }

    private String resolveRawSource(Song song) {
        if (song.getRawSourceContent() != null && !song.getRawSourceContent().isBlank()) {
            return song.getRawSourceContent();
        }
        return song.getRawLyrics() != null ? song.getRawLyrics() : song.getLyrics();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }

    private String buildLearningLyrics(List<LyricNormalizer.NormalizedLine> lines,
                                       List<LyricLineClassifier.Classification> classifications) {
        return String.join("\n", java.util.stream.IntStream.range(0, lines.size())
                .filter(index -> classifications.get(index).lineType() == com.each17.backend.lyric.entity.LyricLineType.LYRIC)
                .mapToObj(index -> lines.get(index).normalizedText())
                .toList());
    }

    private void rebuildSongTokens(Song song, List<LyricLine> lines) {
        if (tokenizationService == null) return;
        lyricTokenRepository.deleteBySongId(song.getId());
        List<com.each17.backend.lyric.entity.LyricToken> tokens = lines.stream()
                .filter(this::isTokenizable)
                .flatMap(line -> tokenizationService.tokenize(line).stream())
                .toList();
        if (!tokens.isEmpty()) lyricTokenRepository.saveAll(tokens);
        if (phraseOccurrenceService != null) phraseOccurrenceService.invalidateSong(song.getId());
    }

    private boolean isTokenizable(LyricLine line) {
        return LyricLineClassifier.isIndexableLine(line);
    }

    private LyricDocumentDto toDocument(Song song, List<LyricLine> lines) {
        return new LyricDocumentDto(
                song.getId(), song.getTitle(), song.getArtist(), song.getAlbum(), resolveRawLyrics(song),
                song.getNormalizedLyrics(), song.getLyricsHash(), song.getImportVersion(), song.getUpdatedAt(),
                lines.stream().map(this::toLineDto).toList(),
                songCreditService == null ? List.of() : songCreditService.findDtos(song.getId())
        );
    }

    private LyricLineDto toLineDto(LyricLine line) {
        return new LyricLineDto(
                line.getId(), line.getLineIndex(), line.getOriginalText(), line.getNormalizedText(),
                line.getLineType(), line.getClassificationSource(), line.getHidden(), line.getConfidence(),
                line.getUserOverride()
        );
    }
}
