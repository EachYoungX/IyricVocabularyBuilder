package com.each17.backend.dictionary.service;

import com.each17.backend.dto.DictionaryEntryDto;
import com.each17.backend.dto.DictionarySourceDto;
import com.each17.backend.common.exception.DictionaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.RowMapper; // 导入 RowMapper
import org.springframework.beans.factory.annotation.Value;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor // 使用 Lombok 进行构造函数注入
public class DictionaryServiceImpl implements DictionaryService {
    private static final int MAX_CACHE_SIZE = 2_000;

    // [核心修复] 不再注入配置字符串，而是直接注入为 dictionaryDataSource 配置的 JdbcTemplate
    @Qualifier("dictionaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;
    @Value("${app.dictionary.enabled:true}")
    private boolean enabled = true;
    private final Map<String, DictionaryEntryDto> lookupCache = new ConcurrentHashMap<>();

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
        if (!enabled) {
            throw new DictionaryNotFoundException(word);
        }
        String normalizedWord = word.toLowerCase();
        DictionaryEntryDto cached = lookupCache.get(normalizedWord);
        if (cached != null) return cached;

        DictionaryEntryDto entry = queryDictionary(normalizedWord);
        if (lookupCache.size() >= MAX_CACHE_SIZE) {
            lookupCache.clear();
        }
        lookupCache.put(normalizedWord, entry);
        return entry;
    }

    private DictionaryEntryDto queryDictionary(String normalizedWord) {
        // 使用参数化查询，防止 SQL 注入
        String sql = "SELECT * FROM dictionary WHERE word = ?";

        // queryForObject 在找不到记录时会抛出 EmptyResultDataAccessException，我们需要处理它
        try {
            return jdbcTemplate.queryForObject(sql, new DictionaryEntryRowMapper(), normalizedWord);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new DictionaryNotFoundException(normalizedWord);
        }
    }

    @Override
    public DictionarySourceDto getSourceInfo() {
        if (!enabled) {
            return DictionarySourceDto.builder()
                    .sourceName("NONE")
                    .dictionaryVersion("disabled")
                    .requiresAttribution(false)
                    .commercialUseAllowed(false)
                    .redistributionAllowed(false)
                    .attributionText("No dictionary is configured for this runtime.")
                    .build();
        }
        return DictionarySourceDto.builder()
                .sourceName("ECDICT")
                .sourceUrl("https://github.com/skywind3000/ECDICT")
                .dictionaryVersion("Bundled SQLite snapshot")
                .importedAt("Local build resource")
                .licenseName("MIT License")
                .requiresAttribution(true)
                .commercialUseAllowed(true)
                .redistributionAllowed(true)
                .attributionText("Dictionary data is derived from the ECDICT open-source project.")
                .build();
    }
}
