//package com.each17.backend.repository;
//
//import com.each17.backend.entity.Song;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//class SongRepositoryTest {
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    @Autowired
//    private SongRepository songRepository;
//
//    @Test
//    void testSaveAndFindById() {
//        // Given
//        Song song = Song.builder()
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        // When
//        Song savedSong = entityManager.persistAndFlush(song);
//        Optional<Song> foundSong = songRepository.findById(savedSong.getId());
//
//        // Then
//        assertThat(foundSong).isPresent();
//        assertThat(foundSong.get().getTitle()).isEqualTo("Yesterday");
//        assertThat(foundSong.get().getArtist()).isEqualTo("The Beatles");
//        assertThat(foundSong.get().getLyrics()).isEqualTo("Yesterday, all my troubles seemed so far away");
//    }
//
//    @Test
//    void testFindByArtist() {
//        // Given
//        Song song1 = Song.builder()
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        Song song2 = Song.builder()
//                .title("Hey Jude")
//                .artist("The Beatles")
//                .lyrics("Hey Jude, don't make it bad")
//                .build();
//
//        Song song3 = Song.builder()
//                .title("Imagine")
//                .artist("John Lennon")
//                .lyrics("Imagine there's no heaven")
//                .build();
//
//        entityManager.persistAndFlush(song1);
//        entityManager.persistAndFlush(song2);
//        entityManager.persistAndFlush(song3);
//
//        // When
//        // Note: This test would require a custom method in the repository
//        // For now, we'll just test findAll
//        List<Song> allSongs = songRepository.findAll();
//
//        // Then
//        assertThat(allSongs).hasSize(3);
//    }
//
//    @Test
//    void testDelete() {
//        // Given
//        Song song = Song.builder()
//                .title("Yesterday")
//                .artist("The Beatles")
//                .lyrics("Yesterday, all my troubles seemed so far away")
//                .build();
//
//        Song savedSong = entityManager.persistAndFlush(song);
//
//        // When
//        songRepository.deleteById(savedSong.getId());
//        entityManager.flush();
//        Optional<Song> foundSong = songRepository.findById(savedSong.getId());
//
//        // Then
//        assertThat(foundSong).isEmpty();
//    }
//}