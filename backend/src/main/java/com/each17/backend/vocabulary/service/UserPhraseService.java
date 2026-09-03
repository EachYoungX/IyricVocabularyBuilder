package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.ValidationException;
import com.each17.backend.dto.UserPhraseRequestDto;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.vocabulary.entity.UserPhrase;
import com.each17.backend.vocabulary.repository.UserPhraseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPhraseService {
    private final UserPhraseRepository repository;

    public List<UserPhrase> list() {
        return repository.findByUserIdOrderByCanonicalPhraseAsc("local");
    }

    public UserPhrase add(UserPhraseRequestDto request) {
        String phrase = normalize(request.canonicalPhrase());
        if (phrase.isBlank() || phrase.split(" ").length < 2) {
            throw new ValidationException("A user phrase must contain at least two tokens");
        }
        return repository.save(UserPhrase.builder().userId("local").canonicalPhrase(phrase)
                .definition(request.definition()).createdAt(LocalDateTime.now().toString()).build());
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NotFoundException("User phrase not found: " + id);
        repository.deleteById(id);
    }

    public List<Match> findMatches(List<LyricToken> tokens) {
        return list().stream().flatMap(phrase -> {
            String[] parts = phrase.getCanonicalPhrase().split(" ");
            return java.util.stream.IntStream.range(0, Math.max(0, tokens.size() - parts.length + 1))
                    .filter(start -> matches(tokens, start, parts))
                    .mapToObj(start -> new Match(phrase, start, start + parts.length - 1));
        }).toList();
    }

    private boolean matches(List<LyricToken> tokens, int start, String[] parts) {
        for (int offset = 0; offset < parts.length; offset++) {
            if (!tokens.get(start + offset).getNormalizedForm().equalsIgnoreCase(parts[offset])) return false;
        }
        return true;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Arrays.stream(value.trim().toLowerCase().split("\\s+"))
                .map(token -> token.replaceAll("^'+|'+$", ""))
                .filter(token -> !token.isBlank()).reduce((left, right) -> left + " " + right).orElse("");
    }

    public record Match(UserPhrase phrase, int start, int end) {
    }
}
