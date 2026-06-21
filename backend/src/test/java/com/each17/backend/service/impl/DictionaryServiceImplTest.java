package com.each17.backend.service.impl;

import com.each17.backend.dto.DictionaryEntryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryServiceImplTest {

    @Mock
    private DataSource dictionaryDataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DictionaryServiceImpl dictionaryService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(dictionaryDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
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

        verify(dictionaryDataSource, times(1)).getConnection();
        verify(connection, times(1)).prepareStatement(anyString());
        verify(preparedStatement, times(1)).setString(1, word.toLowerCase());
        verify(preparedStatement, times(1)).executeQuery();
        verify(resultSet, times(1)).next();
    }

    @Test
    void testLookupWordNotFound() throws SQLException {
        // Given
        String word = "nonexistent";
        when(resultSet.next()).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> dictionaryService.lookupWord(word));

        verify(dictionaryDataSource, times(1)).getConnection();
        verify(connection, times(1)).prepareStatement(anyString());
        verify(preparedStatement, times(1)).setString(1, word.toLowerCase());
        verify(preparedStatement, times(1)).executeQuery();
        verify(resultSet, times(1)).next();
    }

    @Test
    void testLookupWordSQLException() throws SQLException {
        // Given
        String word = "voyage";
        when(dictionaryDataSource.getConnection()).thenThrow(new SQLException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> dictionaryService.lookupWord(word));

        verify(dictionaryDataSource, times(1)).getConnection();
    }
}