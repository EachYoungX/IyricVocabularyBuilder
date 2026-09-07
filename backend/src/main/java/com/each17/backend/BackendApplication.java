package com.each17.backend;

import com.each17.backend.dictionary.service.DictionaryProbeCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendApplication {

    public static void main(String[] args) {
        if (Boolean.parseBoolean(System.getenv("APP_DICTIONARY_PROBE_ONLY"))) {
            int exitCode = DictionaryProbeCommand.run(
                    System.getenv("APP_DICTIONARY_DB_URL"), System.out, System.err);
            System.exit(exitCode);
            return;
        }
        SpringApplication.run(BackendApplication.class, args);
    }
}
