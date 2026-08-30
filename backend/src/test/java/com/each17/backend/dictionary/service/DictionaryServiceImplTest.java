package com.each17.backend.dictionary.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DictionaryServiceImplTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DictionaryServiceImpl service = new DictionaryServiceImpl(jdbcTemplate);

    @Test
    void cachesRepeatedLookups() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("voyage"))).thenAnswer(invocation -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("word")).thenReturn("voyage");
            when(rs.getString("definition")).thenReturn("a long journey");
            when(rs.getString("translation")).thenReturn("航行；旅行");
            when(rs.getString("phonetic")).thenReturn("/v/");
            when(rs.getString("pos")).thenReturn("noun");
            when(rs.getString("forms")).thenReturn("");
            return invocation.<org.springframework.jdbc.core.RowMapper<?>>getArgument(1).mapRow(rs, 0);
        });

        assertEquals("voyage", service.lookupWord("Voyage").getWord());
        assertEquals("voyage", service.lookupWord("voyage").getWord());

        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class), eq("voyage"));
    }

    @Test
    void disabledDictionaryDoesNotQueryDatasource() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThrows(com.each17.backend.common.exception.DictionaryNotFoundException.class,
                () -> service.lookupWord("voyage"));
        verifyNoInteractions(jdbcTemplate);
    }
}
