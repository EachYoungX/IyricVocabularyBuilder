package com.each17.backend.song.service;

import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.SongImportRequestDto;
import com.each17.backend.lyric.service.LyricStructureService;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.mapper.SongMapper;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.UserVocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SongImportTransaction {
    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final LyricStructureService lyricStructureService;
    private final UserVocabularyService userVocabularyService;

    @Transactional
    public void importOne(SongImportRequestDto request, boolean autoAddToPersonalVocabulary) {
        validate(request);
        Song song = songMapper.toEntity(request);
        String sourceContent = resolveSourceContent(request);
        var existing = songRepository.findByTitleAndArtist(song.getTitle(), song.getArtist());
        if (existing.isPresent()) {
            if (!lyricStructureService.isSameContent(existing.get(), sourceContent)) {
                throw new DataIntegrityViolationException("Song exists with different lyrics");
            }
            lyricStructureService.structureSong(existing.get(), sourceContent, false);
            if (autoAddToPersonalVocabulary) {
                userVocabularyService.addDefaultWordsForSong(existing.get().getId());
            }
            return;
        }

        Song savedSong = songRepository.save(song);
        lyricStructureService.structureSong(savedSong, sourceContent, true);
        if (autoAddToPersonalVocabulary) {
            userVocabularyService.addDefaultWordsForSong(savedSong.getId());
        }
    }

    private void validate(SongImportRequestDto request) {
        if (request == null) throw new ValidationException("Song cannot be null");
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("Title cannot be empty");
        }
        if (request.getArtist() == null || request.getArtist().isBlank()) {
            throw new ValidationException("Artist cannot be empty");
        }
        if (request.getLyrics() == null || request.getLyrics().isBlank()) {
            throw new ValidationException("Lyrics cannot be empty");
        }
    }

    private String resolveSourceContent(SongImportRequestDto request) {
        return request.getRawSourceContent() != null && !request.getRawSourceContent().isBlank()
                ? request.getRawSourceContent() : request.getLyrics();
    }
}
