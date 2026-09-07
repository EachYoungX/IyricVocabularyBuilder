package com.each17.backend.dictionary.service;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;

public final class DictionaryProbeCommand {
    private DictionaryProbeCommand() {
    }

    public static int run(String jdbcUrl, PrintStream output, PrintStream error) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            writeJson(error, "invalid", null, null, "APP_DICTIONARY_DB_URL is required");
            return 2;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                DictionaryCompatibility.Report report = DictionaryCompatibility.inspect(connection);
                writeJson(output, "valid", report.schemaVersion(), report.packageVersion(), null);
                return 0;
            }
        } catch (DictionaryCompatibility.ValidationException exception) {
            String status = exception.status() == DictionaryCompatibility.Status.INCOMPATIBLE
                    ? "incompatible" : "invalid";
            writeJson(error, status, null, null, exception.getMessage());
            return exception.status() == DictionaryCompatibility.Status.INCOMPATIBLE ? 3 : 2;
        } catch (Exception exception) {
            writeJson(error, "invalid", null, null, exception.getMessage());
            return 2;
        }
    }

    private static void writeJson(
            PrintStream stream,
            String status,
            String schemaVersion,
            String packageVersion,
            String message
    ) {
        stream.print("{\"status\":\"");
        stream.print(escape(status));
        stream.print("\"");
        if (schemaVersion != null) stream.print(",\"schemaVersion\":\"" + escape(schemaVersion) + "\"");
        if (packageVersion != null) stream.print(",\"datasetVersion\":\"" + escape(packageVersion) + "\"");
        if (message != null) stream.print(",\"message\":\"" + escape(message) + "\"");
        stream.println("}");
    }

    private static String escape(String value) {
        if (value == null) return "unknown error";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
