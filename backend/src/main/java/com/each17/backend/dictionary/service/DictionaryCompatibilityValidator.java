package com.each17.backend.dictionary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DictionaryCompatibilityValidator {
    private final DataSource dataSource;
    private final boolean enabled;

    public DictionaryCompatibilityValidator(
            @org.springframework.beans.factory.annotation.Qualifier("dictionaryDataSource") DataSource dataSource,
            @Value("${app.dictionary.enabled:false}") boolean enabled
    ) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        try (Connection connection = dataSource.getConnection()) {
            DictionaryCompatibility.inspect(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot open lyric dictionary", exception);
        }
    }
}
