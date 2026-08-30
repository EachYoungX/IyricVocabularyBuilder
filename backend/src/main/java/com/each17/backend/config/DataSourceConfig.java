package com.each17.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataSourceConfig {

    /**
     * 手动配置主数据源 (app_data.db)。
     * 通过创建我们自己的 @Primary DataSource Bean，我们完全接管了主数据源的控制权，
     * 绕过了所有可能出问题的 Spring Boot 自动配置。
     */
    @Bean(name = "appDataSource")
    @Primary
    public DataSource appDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            ResourceLoader resourceLoader
    ) {
        System.out.println("====================================================");
        System.out.println("      MANUALLY CONFIGURING PRIMARY DATASOURCE       ");
        System.out.println("      URL: " + url);
        System.out.println("====================================================");

        // 1. 手动创建 DataSource 实例
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);

        // 2. 手动执行 schema.sql 初始化
        Resource schema = resourceLoader.getResource("classpath:schema.sql");
        if (schema.exists()) {
            System.out.println(">>> schema.sql found. Executing initialization script...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
            populator.execute(dataSource);
            migrateSongColumns(dataSource);
            migrateVocabularyColumns(dataSource);
            migrateLyricTokenColumns(dataSource);
            System.out.println(">>> schema.sql execution finished.");
        } else {
            System.err.println("!!! WARNING: schema.sql not found! Tables will not be created.");
        }

        return dataSource;
    }

    private void migrateSongColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(songs)",
                (rs, rowNum) -> rs.getString("name")
        ));

        addColumnIfMissing(jdbcTemplate, columns, "raw_lyrics", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "raw_title", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "raw_artist", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "normalized_lyrics", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "lyrics_hash", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "import_version", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(jdbcTemplate, columns, "updated_at", "TEXT");

        jdbcTemplate.update("UPDATE songs SET raw_lyrics = lyrics WHERE raw_lyrics IS NULL");
        jdbcTemplate.update("UPDATE songs SET raw_title = title WHERE raw_title IS NULL");
        jdbcTemplate.update("UPDATE songs SET raw_artist = artist WHERE raw_artist IS NULL");
        jdbcTemplate.update("UPDATE songs SET normalized_lyrics = raw_lyrics WHERE normalized_lyrics IS NULL");
        jdbcTemplate.update("UPDATE songs SET import_version = 1 WHERE import_version IS NULL");
        jdbcTemplate.update("UPDATE songs SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, Set<String> columns, String name, String definition) {
        if (!columns.contains(name)) {
            jdbcTemplate.execute("ALTER TABLE songs ADD COLUMN " + name + " " + definition);
            columns.add(name);
        }
    }

    private void migrateVocabularyColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(vocabulary)",
                (rs, rowNum) -> rs.getString("name")
        ));

        addVocabularyColumnIfMissing(jdbcTemplate, columns, "display_forms", "TEXT");
        addVocabularyColumnIfMissing(jdbcTemplate, columns, "occurrence_count", "INTEGER NOT NULL DEFAULT 0");
        addVocabularyColumnIfMissing(jdbcTemplate, columns, "song_count", "INTEGER NOT NULL DEFAULT 0");
        addVocabularyColumnIfMissing(jdbcTemplate, columns, "learning_score", "REAL NOT NULL DEFAULT 1.0");
        addVocabularyColumnIfMissing(jdbcTemplate, columns, "recommended", "INTEGER NOT NULL DEFAULT 1");
    }

    private void addVocabularyColumnIfMissing(JdbcTemplate jdbcTemplate, Set<String> columns, String name, String definition) {
        if (!columns.contains(name)) {
            jdbcTemplate.execute("ALTER TABLE vocabulary ADD COLUMN " + name + " " + definition);
            columns.add(name);
        }
    }

    private void migrateLyricTokenColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(lyric_tokens)",
                (rs, rowNum) -> rs.getString("name")
        ));

        addLyricTokenColumnIfMissing(jdbcTemplate, columns, "token_position", "INTEGER NOT NULL DEFAULT 0");
        addLyricTokenColumnIfMissing(jdbcTemplate, columns, "lemma_status", "TEXT NOT NULL DEFAULT 'FALLBACK'");

        jdbcTemplate.execute("""
                WITH ranked AS (
                    SELECT id,
                           ROW_NUMBER() OVER (
                               PARTITION BY lyric_line_id
                               ORDER BY start_offset, end_offset, id
                           ) - 1 AS position
                    FROM lyric_tokens
                )
                UPDATE lyric_tokens
                SET token_position = (
                    SELECT position FROM ranked WHERE ranked.id = lyric_tokens.id
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_lyric_tokens_line_position "
                + "ON lyric_tokens(lyric_line_id, token_position)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_lyric_tokens_normalized "
                + "ON lyric_tokens(normalized_form)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_lyric_tokens_lemma_location "
                + "ON lyric_tokens(lemma, lyric_line_id, token_position)");
    }

    private void addLyricTokenColumnIfMissing(JdbcTemplate jdbcTemplate, Set<String> columns, String name, String definition) {
        if (!columns.contains(name)) {
            jdbcTemplate.execute("ALTER TABLE lyric_tokens ADD COLUMN " + name + " " + definition);
            columns.add(name);
        }
    }

    /**
     * 手动配置第二数据源 (dictionary.sqlite)。
     */
    @Bean(name = "dictionaryDataSource")
    public DataSource dictionaryDataSource(
            @Value("${app.dictionary.datasource.url}") String url,
            @Value("${app.dictionary.datasource.driver-class-name}") String driverClassName
    ) {
        System.out.println("====================================================");
        System.out.println("     MANUALLY CONFIGURING DICTIONARY DATASOURCE     ");
        System.out.println("      URL: " + url);
        System.out.println("====================================================");

        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    // 为词典数据源创建 JdbcTemplate
    @Bean(name = "dictionaryJdbcTemplate")
    public JdbcTemplate dictionaryJdbcTemplate(@Qualifier("dictionaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
