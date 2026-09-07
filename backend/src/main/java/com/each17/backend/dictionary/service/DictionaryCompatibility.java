package com.each17.backend.dictionary.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class DictionaryCompatibility {
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    private static final Map<String, Set<String>> REQUIRED_SCHEMA = Map.of(
            "word_entry", Set.of(
                    "word", "phonetic", "definition_en", "translation_zh", "pos_profile",
                    "collins_star", "oxford_core", "tags", "bnc_rank", "coca_rank", "morphology"),
            "phrase_entry", Set.of(
                    "id", "source_pattern", "canonical_pattern", "definition_en", "definition_zh",
                    "usage_note_zh", "phrase_type", "source", "token_count_min", "token_count_max",
                    "match_priority"),
            "phrase_pattern_token", Set.of(
                    "phrase_id", "pattern_position", "token_type", "match_type", "match_value",
                    "slot_hint", "min_tokens", "max_tokens"),
            "phrase_anchor", Set.of("phrase_id", "anchor_position", "anchor_type", "anchor_value"),
            "dictionary_meta", Set.of("key", "value")
    );

    private DictionaryCompatibility() {
    }

    public static Report inspect(Connection connection) {
        try {
            for (Map.Entry<String, Set<String>> table : REQUIRED_SCHEMA.entrySet()) {
                assertTableColumns(connection, table.getKey(), table.getValue());
            }
            Map<String, String> metadata = readMetadata(connection);
            String schemaVersion = requiredMetadata(metadata, "schema.version");
            if (!SUPPORTED_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new ValidationException(Status.INCOMPATIBLE,
                        "Unsupported lyric dictionary schema.version: " + schemaVersion);
            }
            String packageVersion = requiredMetadata(metadata, "package.version");
            return new Report(schemaVersion, packageVersion);
        } catch (ValidationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new ValidationException(Status.INVALID,
                    "Cannot read lyric dictionary: " + exception.getMessage(), exception);
        }
    }

    private static void assertTableColumns(Connection connection, String table, Set<String> required)
            throws SQLException {
        Set<String> columns = new java.util.HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(\"" + table + "\")")) {
            while (rows.next()) columns.add(rows.getString("name"));
        }
        if (columns.isEmpty()) {
            throw new ValidationException(Status.INVALID, "Missing lyric dictionary table: " + table);
        }
        for (String column : required) {
            if (!columns.contains(column)) {
                throw new ValidationException(Status.INVALID,
                        "Missing lyric dictionary column: " + table + "." + column);
            }
        }
    }

    private static Map<String, String> readMetadata(Connection connection) throws SQLException {
        Map<String, String> metadata = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT key, value FROM dictionary_meta ORDER BY key")) {
            while (rows.next()) metadata.put(rows.getString("key"), rows.getString("value"));
        }
        return metadata;
    }

    private static String requiredMetadata(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            throw new ValidationException(Status.INVALID, "Missing lyric dictionary metadata: " + key);
        }
        return value;
    }

    public enum Status {
        INVALID,
        INCOMPATIBLE
    }

    public record Report(String schemaVersion, String packageVersion) {
    }

    public static final class ValidationException extends IllegalStateException {
        private final Status status;

        public ValidationException(Status status, String message) {
            super(message);
            this.status = status;
        }

        public ValidationException(Status status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public Status status() {
            return status;
        }
    }
}
