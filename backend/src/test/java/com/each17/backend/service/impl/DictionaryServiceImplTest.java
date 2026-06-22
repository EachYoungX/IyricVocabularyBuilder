package com.each17.backend.service.impl;

import com.each17.backend.dictionary.service.DictionaryServiceImpl;
import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.common.exception.DictionaryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DictionaryServiceImpl dictionaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLookupWord() throws SQLException {
        // Given
        String word = "voyage";
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("word")).thenReturn("voyage");
        when(resultSet.getString("phonetic")).thenReturn("/ˈvɔɪ.ɪdʒ/");
        when(resultSet.getString("definition")).thenReturn("a long journey, especially by ship or in space");
        when(resultSet.getString("translation")).thenReturn("航行；旅行");
        when(resultSet.getString("pos")).thenReturn("noun, verb");
        when(resultSet.getInt("collins_star")).thenReturn(5);
        when(resultSet.getInt("bnc_rank")).thenReturn(3025);
        when(resultSet.getInt("frq_rank")).thenReturn(2632);
        when(resultSet.getString("forms")).thenReturn("p:voyaged/d:voyaged/i:voyaging/s:voyages");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(word)))
                .thenAnswer(invocation -> {
                    RowMapper<DictionaryEntryDto> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(resultSet, 0);
                });

        // When
        DictionaryEntryDto result = dictionaryService.lookupWord(word);

        // Then
        assertNotNull(result);
        assertEquals("voyage", result.getWord());
        assertEquals("/ˈvɔɪ.ɪdʒ/", result.getPhonetic());
        assertEquals("a long journey, especially by ship or in space", result.getDefinition());
        assertEquals("航行；旅行", result.getTranslation());
        assertEquals("noun, verb", result.getPos());
        assertEquals(Integer.valueOf(5), result.getCollins());
        assertEquals(Integer.valueOf(3025), result.getBnc());
        assertEquals(Integer.valueOf(2632), result.getFrq());
        assertEquals("p:voyaged/d:voyaged/i:voyaging/s:voyages", result.getForms());

        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(word));
    }

    @Test
    void testLookupWordNotFound() {
        // Given
        String word = "nonexistent";
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(word)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When & Then
        assertThrows(DictionaryNotFoundException.class, () -> dictionaryService.lookupWord(word));
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(word));
    }

    @Test
    void testLookupWordSQLException() {
        // Given
        String word = "voyage";
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(word)))
                .thenThrow(new DataAccessResourceFailureException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> dictionaryService.lookupWord(word));

        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(word));
    }
}
