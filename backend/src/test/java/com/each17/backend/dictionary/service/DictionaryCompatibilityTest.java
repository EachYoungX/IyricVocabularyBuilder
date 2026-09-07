package com.each17.backend.dictionary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryCompatibilityTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesTheSchemaUsedByRuntimeDictionaryQueries() throws Exception {
        Path database = createValidDictionary();

        try (Connection connection = DriverManager.getConnection(readOnlyUrl(database))) {
            DictionaryCompatibility.Report report = DictionaryCompatibility.inspect(connection);
            assertEquals("1", report.schemaVersion());
            assertEquals("2026.09", report.packageVersion());
        }
    }

    @Test
    void rejectsMissingPhraseSchemaAsInvalid() throws Exception {
        Path database = createValidDictionary();
        execute(database, "DROP TABLE phrase_anchor");

        try (Connection connection = DriverManager.getConnection(readOnlyUrl(database))) {
            DictionaryCompatibility.ValidationException exception = assertThrows(
                    DictionaryCompatibility.ValidationException.class,
                    () -> DictionaryCompatibility.inspect(connection));
            assertEquals(DictionaryCompatibility.Status.INVALID, exception.status());
            assertTrue(exception.getMessage().contains("phrase_anchor"));
        }
    }

    @Test
    void classifiesUnsupportedSchemaVersionsAsIncompatible() throws Exception {
        Path database = createValidDictionary();
        execute(database, "UPDATE dictionary_meta SET value = '2' WHERE key = 'schema.version'");

        try (Connection connection = DriverManager.getConnection(readOnlyUrl(database))) {
            DictionaryCompatibility.ValidationException exception = assertThrows(
                    DictionaryCompatibility.ValidationException.class,
                    () -> DictionaryCompatibility.inspect(connection));
            assertEquals(DictionaryCompatibility.Status.INCOMPATIBLE, exception.status());
        }
    }

    @Test
    void probeModeWritesMachineReadableMetadata() throws Exception {
        Path database = createValidDictionary();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = DictionaryProbeCommand.run(
                readOnlyUrl(database), new PrintStream(output), new PrintStream(error));

        assertEquals(0, exitCode);
        assertEquals("", error.toString(StandardCharsets.UTF_8));
        String json = output.toString(StandardCharsets.UTF_8);
        assertTrue(json.contains("\"status\":\"valid\""));
        assertTrue(json.contains("\"schemaVersion\":\"1\""));
        assertTrue(json.contains("\"datasetVersion\":\"2026.09\""));
    }

    private Path createValidDictionary() throws Exception {
        Path database = temporaryDirectory.resolve("dictionary-" + UUID.randomUUID() + ".sqlite");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE word_entry(
                        word TEXT, phonetic TEXT, definition_en TEXT, translation_zh TEXT,
                        pos_profile TEXT, collins_star INTEGER, oxford_core INTEGER, tags TEXT,
                        bnc_rank INTEGER, coca_rank INTEGER, morphology TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE phrase_entry(
                        id INTEGER, source_pattern TEXT, canonical_pattern TEXT, definition_en TEXT,
                        definition_zh TEXT, usage_note_zh TEXT, phrase_type TEXT, source TEXT,
                        token_count_min INTEGER, token_count_max INTEGER, match_priority INTEGER
                    )
                    """);
            statement.execute("""
                    CREATE TABLE phrase_pattern_token(
                        phrase_id INTEGER, pattern_position INTEGER, token_type TEXT, match_type TEXT,
                        match_value TEXT, slot_hint TEXT, min_tokens INTEGER, max_tokens INTEGER
                    )
                    """);
            statement.execute("""
                    CREATE TABLE phrase_anchor(
                        phrase_id INTEGER, anchor_position INTEGER, anchor_type TEXT, anchor_value TEXT
                    )
                    """);
            statement.execute("CREATE TABLE dictionary_meta(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.execute("INSERT INTO dictionary_meta(key, value) VALUES ('schema.version', '1')");
            statement.execute("INSERT INTO dictionary_meta(key, value) VALUES ('package.version', '2026.09')");
        }
        return database;
    }

    private void execute(Path database, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String readOnlyUrl(Path database) {
        return "jdbc:sqlite:file:" + database.toAbsolutePath().toString().replace('\\', '/') + "?mode=ro";
    }
}
