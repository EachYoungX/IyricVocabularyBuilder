package com.each17.backend.repository;

import com.each17.backend.entity.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VocabularyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Test
    void testSaveAndFindById() {
        // Given
        String occurrencesJson = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        Vocabulary vocabulary = Vocabulary.builder()
                .word("yesterday")
                .occurrences(occurrencesJson)
                .build();

        // When
        Vocabulary savedVocabulary = entityManager.persistAndFlush(vocabulary);
        Optional<Vocabulary> foundVocabulary = vocabularyRepository.findById("yesterday");

        // Then
        assertThat(foundVocabulary).isPresent();
        assertThat(foundVocabulary.get().getWord()).isEqualTo("yesterday");
        assertThat(foundVocabulary.get().getOccurrences()).isEqualTo(occurrencesJson);
    }

    @Test
    void testFindByWordStartingWith() {
        // Given
        String occurrencesJson1 = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        String occurrencesJson2 = "[{\"songTitle\":\"Love Me Do\",\"lyricLine\":\"Love, love me do\"}]";
        String occurrencesJson3 = "[{\"songTitle\":\"Hey Jude\",\"lyricLine\":\"Hey Jude, don't make it bad\"}]";
        
        Vocabulary vocab1 = Vocabulary.builder()
                .word("yesterday")
                .occurrences(occurrencesJson1)
                .build();
                
        Vocabulary vocab2 = Vocabulary.builder()
                .word("love")
                .occurrences(occurrencesJson2)
                .build();
                
        Vocabulary vocab3 = Vocabulary.builder()
                .word("hey")
                .occurrences(occurrencesJson3)
                .build();

        entityManager.persistAndFlush(vocab1);
        entityManager.persistAndFlush(vocab2);
        entityManager.persistAndFlush(vocab3);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vocabulary> result = vocabularyRepository.findByWordStartingWithOrderByWordAsc("y", pageable);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getWord()).isEqualTo("yesterday");
    }

    @Test
    void testDelete() {
        // Given
        String occurrencesJson = "[{\"songTitle\":\"Yesterday\",\"lyricLine\":\"Yesterday, all my troubles seemed so far away\"}]";
        Vocabulary vocabulary = Vocabulary.builder()
                .word("yesterday")
                .occurrences(occurrencesJson)
                .build();

        Vocabulary savedVocabulary = entityManager.persistAndFlush(vocabulary);

        // When
        vocabularyRepository.deleteById("yesterday");
        entityManager.flush();
        Optional<Vocabulary> foundVocabulary = vocabularyRepository.findById("yesterday");

        // Then
        assertThat(foundVocabulary).isEmpty();
    }
}