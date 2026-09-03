package com.each17.backend.lyric.service;

import com.each17.backend.common.exception.ConflictException;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.repository.SongRepository;
import com.each17.backend.vocabulary.service.VocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LyricStructureServiceTest {
    @Mock SongRepository songRepository;
    @Mock LyricLineRepository lyricLineRepository;
    @Mock LyricTokenRepository lyricTokenRepository;
    @Mock VocabularyService vocabularyService;

    private LyricStructureService service;
    private final LyricNormalizer normalizer = new LyricNormalizer();
    private final LyricsHashService hashService = new LyricsHashService();

    @BeforeEach
    void setUp() {
        service = new LyricStructureService(
                songRepository, lyricLineRepository, normalizer,
                new LyricLineClassifier(), hashService, vocabularyService, lyricTokenRepository
        );
        lenient().when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(lyricLineRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void returnsExistingLinesWhenContentHashIsUnchanged() {
        Song song = song("hello", hashService.hash("hello"));
        when(lyricLineRepository.existsBySongId(1L)).thenReturn(true);
        when(lyricLineRepository.findBySongIdOrderByLineIndexAsc(1L)).thenReturn(List.of());

        service.structureSong(song, "hello", false);

        verify(lyricLineRepository, never()).deleteBySongId(anyLong());
        verify(lyricTokenRepository, never()).deleteBySongId(anyLong());
        verify(songRepository, never()).save(any());
    }

    @Test
    void rejectsDifferentLyricsWithoutExplicitOverwrite() {
        Song song = song("old", hashService.hash("old"));
        assertThrows(ConflictException.class, () -> service.structureSong(song, "new", false));
    }

    @Test
    void preservesUserOverrideWhenMatchingOriginalLineIsRebuilt() {
        Song song = song("John:", hashService.hash("old"));
        LyricLine override = LyricLine.builder()
                .song(song).lineIndex(0).originalText("John:").normalizedText("John:")
                .lineType(LyricLineType.LYRIC).hidden(false).confidence(1.0).userOverride(true)
                .build();
        when(lyricLineRepository.findBySongIdOrderByLineIndexAsc(1L)).thenReturn(List.of(override));

        var document = service.structureSong(song, "John:\nA new line", true);

        assertEquals(2, document.lines().size());
        assertEquals(LyricLineType.LYRIC, document.lines().getFirst().lineType());
        assertFalse(document.lines().getFirst().hidden());
        assertTrue(document.lines().getFirst().userOverride());
        assertEquals(2, song.getImportVersion());
    }

    @Test
    void preservesOriginalLyricsWhenRebuildingLearningText() {
        Song song = song("Original lyrics", hashService.hash("old"));
        when(lyricLineRepository.findBySongIdOrderByLineIndexAsc(1L)).thenReturn(List.of());

        service.structureSong(song, "Edited learning lyrics", true, true);

        assertEquals("Original lyrics", song.getRawLyrics());
        assertEquals("Edited learning lyrics", song.getLyrics());
    }

    @Test
    void previewsParsedResultFromRawSourceAfterLearningTextWasEdited() {
        String rawSource = "[ti:There For You]\n"
                + "[ar:Martin Garrix/Troye Sivan]\n"
                + "[al:There For You]\n"
                + "[00:00.00]There For You (为你在此) - Martin Garrix (马丁·盖瑞斯)/Troye Sivan (特洛耶·希文)\n"
                + "[00:00.50]I woke up pissed off today\n"
                + "[00:01.00]And lately everyone feels fake";
        Song song = song("Edited learning text", hashService.hash("edited"));
        song.setRawSourceContent(rawSource);
        song.setRawLyrics(rawSource);
        when(songRepository.findById(1L)).thenReturn(Optional.of(song));

        var document = service.previewRawSource(1L);

        assertEquals("There For You", document.title());
        assertEquals("Martin Garrix/Troye Sivan", document.artist());
        assertEquals(LyricLineType.METADATA, document.lines().getFirst().lineType());
        assertEquals(LyricLineType.LYRIC, document.lines().get(1).lineType());
        assertEquals("I woke up pissed off today\nAnd lately everyone feels fake", document.normalizedLyrics());
        verify(lyricLineRepository, never()).deleteBySongId(anyLong());
        verify(lyricTokenRepository, never()).deleteBySongId(anyLong());
        verify(songRepository, never()).save(any());
    }

    private Song song(String lyrics, String hash) {
        return Song.builder()
                .id(1L).title("Title").artist("Artist").lyrics(lyrics).rawLyrics(lyrics)
                .normalizedLyrics(lyrics).lyricsHash(hash).importVersion(1)
                .build();
    }
}
