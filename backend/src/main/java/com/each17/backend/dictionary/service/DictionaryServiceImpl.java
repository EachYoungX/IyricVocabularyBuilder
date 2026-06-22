package com.each17.backend.dictionary.service;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.common.exception.DictionaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.RowMapper; // 导入 RowMapper

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor // 使用 Lombok 进行构造函数注入
public class DictionaryServiceImpl implements DictionaryService {

    // [核心修复] 不再注入配置字符串，而是直接注入为 dictionaryDataSource 配置的 JdbcTemplate
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    // 定义一个可复用的 RowMapper，用于将数据库查询结果映射到 DTO
    private static final class DictionaryEntryRowMapper implements RowMapper<DictionaryEntryDto> {
        @Override
        public DictionaryEntryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DictionaryEntryDto.builder()
                    .word(rs.getString("word"))
                    .phonetic(rs.getString("phonetic"))
                    .definition(rs.getString("definition"))
                    .translation(rs.getString("translation"))
                    .pos(rs.getString("pos"))
                    .collins(rs.getInt("collins_star"))
                    .bnc(rs.getInt("bnc_rank"))
                    .frq(rs.getInt("frq_rank"))
                    .forms(rs.getString("forms"))
                    .build();
        }
    }

    @Override
    public DictionaryEntryDto lookupWord(String word) {
        // 使用参数化查询，防止 SQL 注入
        String sql = "SELECT * FROM dictionary WHERE word = ?";

        // queryForObject 在找不到记录时会抛出 EmptyResultDataAccessException，我们需要处理它
        try {
            return jdbcTemplate.queryForObject(sql, new DictionaryEntryRowMapper(), word.toLowerCase());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new DictionaryNotFoundException(word);
        }
    }
}
