package com.each17.backend.vocabulary.service;

import com.each17.backend.dictionary.service.DictionaryMetadataRepository;
import com.each17.backend.dictionary.service.PhraseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PhraseOccurrenceServiceTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DictionaryMetadataRepository metadataRepository = mock(DictionaryMetadataRepository.class);
    private final PhraseRepository phraseRepository = mock(PhraseRepository.class);
    private final PhraseOccurrenceCacheWriter cacheWriter = mock(PhraseOccurrenceCacheWriter.class);
    private final PhraseOccurrenceService service = new PhraseOccurrenceService(
            jdbcTemplate, metadataRepository, phraseRepository, cacheWriter);

    @Test
    void emptyMatchResultIsCachedByPersistentSongState() throws Exception {
        PhraseCacheVersion version = new PhraseCacheVersion("dictionary-1", "tokenizer-1", "lemma-1");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class),
                eq(7L), eq("dictionary-1"), eq("tokenizer-1"), eq("lemma-1")))
                .thenReturn(0, 1);

        Method ensureSongCached = PhraseOccurrenceService.class
                .getDeclaredMethod("ensureSongCached", Long.class, PhraseCacheVersion.class);
        ensureSongCached.setAccessible(true);
        ensureSongCached.invoke(service, 7L, version);
        ensureSongCached.invoke(service, 7L, version);

        verify(cacheWriter, times(1)).refreshSong(7L, version);
    }

    @Test
    void invalidatingOneSongDoesNotClearOtherSongs() {
        service.invalidateSong(9L);

        verify(jdbcTemplate).update("DELETE FROM phrase_occurrence WHERE song_id = ?", 9L);
        verify(jdbcTemplate).update("DELETE FROM phrase_cache_state WHERE song_id = ?", 9L);
        verify(jdbcTemplate, never()).update("DELETE FROM phrase_occurrence");
        verify(jdbcTemplate, never()).update("DELETE FROM phrase_cache_state");
    }
}
