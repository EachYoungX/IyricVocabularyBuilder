package com.each17.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Set;

@Configuration
@Slf4j
public class DataSourceConfig {

    private static final int SQLITE_BUSY_TIMEOUT_MS = 5_000;

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
        log.info("Configuring primary SQLite datasource: {}", url);
        HikariDataSource dataSource = sqliteDataSource(
                "app-sqlite-pool", url, driverClassName, 1, true, false);

        // 2. 手动执行 schema.sql 初始化
        Resource schema = resourceLoader.getResource("classpath:schema.sql");
        if (schema.exists()) {
            log.info("Initializing and migrating the application database");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
            populator.execute(dataSource);
            migrateSongColumns(dataSource);
            migrateSongCreditTable(dataSource);
            migrateVocabularyColumns(dataSource);
            migrateLyricLineColumns(dataSource);
            migrateLyricTokenColumns(dataSource);
            log.info("Application database initialization finished");
        } else {
            log.warn("schema.sql was not found; application tables were not initialized");
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
        addColumnIfMissing(jdbcTemplate, columns, "album", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "raw_source_content", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "normalized_lyrics", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "lyrics_hash", "TEXT");
        addColumnIfMissing(jdbcTemplate, columns, "import_version", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(jdbcTemplate, columns, "updated_at", "TEXT");

        jdbcTemplate.update("UPDATE songs SET raw_lyrics = lyrics WHERE raw_lyrics IS NULL");
        jdbcTemplate.update("UPDATE songs SET raw_source_content = COALESCE(raw_lyrics, lyrics) WHERE raw_source_content IS NULL");
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

    private void migrateSongCreditTable(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS song_credit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    song_id INTEGER NOT NULL,
                    credit_type TEXT NOT NULL,
                    credit_label TEXT,
                    credit_value TEXT NOT NULL,
                    source_line_id INTEGER,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE,
                    FOREIGN KEY(source_line_id) REFERENCES lyric_lines(id) ON DELETE SET NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_song_credit_song ON song_credit(song_id, sort_order, id)");
    }

    private void migrateLyricTokenColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(lyric_tokens)",
                (rs, rowNum) -> rs.getString("name")
        ));

        addLyricTokenColumnIfMissing(jdbcTemplate, columns, "token_position", "INTEGER NOT NULL DEFAULT 0");
        addLyricTokenColumnIfMissing(jdbcTemplate, columns, "lemma_status", "TEXT NOT NULL DEFAULT 'FALLBACK'");

        String migrationKey = "schema.lyric-token-position";
        String migrated = jdbcTemplate.query(
                "SELECT value FROM app_meta WHERE key = ?",
                ps -> ps.setString(1, migrationKey),
                rs -> rs.next() ? rs.getString(1) : null);
        if (!"1".equals(migrated)) {
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
            jdbcTemplate.update("INSERT OR REPLACE INTO app_meta(key, value) VALUES (?, '1')", migrationKey);
        }
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_lyric_tokens_line_position "
                + "ON lyric_tokens(lyric_line_id, token_position)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_lyric_tokens_normalized "
                + "ON lyric_tokens(normalized_form)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_lyric_tokens_lemma_location "
                + "ON lyric_tokens(lemma, lyric_line_id, token_position)");
    }

    private void migrateLyricLineColumns(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(lyric_lines)",
                (rs, rowNum) -> rs.getString("name")
        ));

        if (!columns.contains("classification_source")) {
            jdbcTemplate.execute("ALTER TABLE lyric_lines ADD COLUMN classification_source TEXT NOT NULL DEFAULT 'DEFAULT'");
        }
        jdbcTemplate.update("UPDATE lyric_lines SET classification_source = 'DEFAULT' "
                + "WHERE classification_source IS NULL OR TRIM(classification_source) = ''");
    }

    private void addLyricTokenColumnIfMissing(JdbcTemplate jdbcTemplate, Set<String> columns, String name, String definition) {
        if (!columns.contains(name)) {
            jdbcTemplate.execute("ALTER TABLE lyric_tokens ADD COLUMN " + name + " " + definition);
            columns.add(name);
        }
    }

    /**
     * 手动配置由运行环境单独提供的只读词典数据源。
     */
    @Bean(name = "dictionaryDataSource")
    public DataSource dictionaryDataSource(
            @Value("${app.dictionary.datasource.url}") String url,
            @Value("${app.dictionary.datasource.driver-class-name}") String driverClassName
    ) {
        log.info("Configuring dictionary SQLite datasource: {}", url);
        return sqliteDataSource("dictionary-sqlite-pool", url, driverClassName, 2, false, true);
    }

    @Bean(name = "appJdbcTemplate")
    public JdbcTemplate appJdbcTemplate(@Qualifier("appDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // 为词典数据源创建 JdbcTemplate
    @Bean(name = "dictionaryJdbcTemplate")
    public JdbcTemplate dictionaryJdbcTemplate(@Qualifier("dictionaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private HikariDataSource sqliteDataSource(
            String poolName,
            String url,
            String driverClassName,
            int maximumPoolSize,
            boolean foreignKeys,
            boolean queryOnly
    ) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10_000);
        config.addDataSourceProperty("busy_timeout", Integer.toString(SQLITE_BUSY_TIMEOUT_MS));
        config.addDataSourceProperty("foreign_keys", Boolean.toString(foreignKeys));
        config.addDataSourceProperty("query_only", Boolean.toString(queryOnly));
        return new HikariDataSource(config);
    }
}
