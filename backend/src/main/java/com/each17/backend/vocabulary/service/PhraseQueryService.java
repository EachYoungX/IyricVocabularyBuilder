package com.each17.backend.vocabulary.service;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dictionary.service.DictionaryService;
import com.each17.backend.dictionary.service.PhraseRepository;
import com.each17.backend.dto.LyricTokenContextDto;
import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.dto.PhraseOccurrenceDto;
import com.each17.backend.dto.PhrasePageDto;
import com.each17.backend.lyric.entity.LyricToken;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.repository.LyricLineRepository;
import com.each17.backend.lyric.repository.LyricTokenRepository;
import com.each17.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PhraseQueryService {
    private final LyricLineRepository lyricLineRepository;
    private final LyricTokenRepository lyricTokenRepository;
    private final PhraseMatcher phraseMatcher;
    private final PhraseRepository phraseRepository;
    private final PhraseOccurrenceService occurrenceService;
    private final DictionaryService dictionaryService;
    private final UserPhraseService userPhraseService;

    public LyricTokenContextDto getTokenContext(Long lineId, int position) {
        List<LyricToken> tokens = lyricTokenRepository.findByLyricLineIdOrderByTokenPositionAsc(lineId);
        if (position < 0 || position >= tokens.size()) {
            throw new NotFoundException("Lyric token not found: " + lineId + ":" + position);
        }
        LyricToken token = tokens.get(position);
        return LyricTokenContextDto.builder()
                .lyricLineId(lineId).tokenPosition(token.getTokenPosition())
                .surfaceForm(token.getSurfaceForm()).normalizedForm(token.getNormalizedForm())
                .lemma(token.getLemma()).lemmaStatus(token.getLemmaStatus()).tokenType(token.getTokenType())
                .startOffset(token.getStartOffset()).endOffset(token.getEndOffset())
                .wordEntry(dictionaryService.findWord(token.getLemma()).orElse(null))
                .phraseMatches(withUserPhrases(phraseMatcher.findMatches(tokens, position), tokens, position))
                .build();
    }

    public List<PhraseMatchDto> getSongPhrases(Long songId) {
        List<PhraseMatchDto> matches = new java.util.ArrayList<>(occurrenceService.findSongMatches(songId));
        for (LyricLine line : lyricLineRepository.findBySongIdOrderByLineIndexAsc(songId)) {
            List<LyricToken> tokens = lyricTokenRepository.findByLyricLineIdOrderByTokenPositionAsc(line.getId());
            matches.addAll(userPhraseMatches(tokens));
        }
        return matches;
    }

    public void refreshSongPhrases(Long songId) {
        occurrenceService.refreshSong(songId);
    }

    public List<PhraseEntry> searchPhrases(String query, int limit) {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        return matchedPhrases(query).stream().limit(limit).toList();
    }

    public PhrasePageDto getPhrasePage(String query, int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ValidationException("Page must be >= 0 and size must be between 1 and 200");
        }
        List<PhraseEntry> matchedPhrases = matchedPhrases(query);
        long totalElements = matchedPhrases.size();
        int fromIndex = Math.min(page * size, matchedPhrases.size());
        int toIndex = Math.min(fromIndex + size, matchedPhrases.size());
        return PhrasePageDto.builder()
                .content(matchedPhrases.subList(fromIndex, toIndex))
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .number(page)
                .size(size)
                .build();
    }

    private List<PhraseEntry> matchedPhrases(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return phraseRepository.findByIds(occurrenceService.findMatchedPhraseIds()).stream()
                .filter(phrase -> normalizedQuery.isBlank()
                        || phrase.canonicalPattern().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || phrase.sourcePattern().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(java.util.Comparator.comparingInt(PhraseEntry::matchPriority).reversed()
                        .thenComparing(java.util.Comparator.comparingInt(PhraseEntry::tokenCountMax).reversed())
                        .thenComparing(PhraseEntry::canonicalPattern))
                .toList();
    }

    public List<PhraseOccurrenceDto> getPhraseOccurrences(Long phraseId) {
        if (phraseId == null || phraseId < 1) throw new ValidationException("phraseId must be positive");
        if (phraseRepository.findById(phraseId).isEmpty()) {
            throw new NotFoundException("Phrase not found: " + phraseId);
        }
        return occurrenceService.findPhraseOccurrences(phraseId);
    }

    private List<PhraseMatchDto> withUserPhrases(List<PhraseMatchDto> matches, List<LyricToken> tokens,
                                                 int selectedPosition) {
        java.util.ArrayList<PhraseMatchDto> result = new java.util.ArrayList<>(matches);
        result.addAll(userPhraseMatches(tokens).stream()
                .filter(match -> match.getStartTokenPosition() <= selectedPosition
                        && selectedPosition <= match.getEndTokenPosition())
                .toList());
        return result;
    }

    private List<PhraseMatchDto> userPhraseMatches(List<LyricToken> tokens) {
        return userPhraseService.findMatches(tokens).stream().map(match -> {
            String surface = tokens.subList(match.start(), match.end() + 1).stream()
                    .map(LyricToken::getSurfaceForm).collect(java.util.stream.Collectors.joining(" "));
            return PhraseMatchDto.builder().phraseId(null).sourcePattern(match.phrase().getCanonicalPhrase())
                    .canonicalPattern(match.phrase().getCanonicalPhrase()).definitionEn(match.phrase().getDefinition())
                    .phraseType("USER_DEFINED").source("USER_DEFINED").startTokenPosition(match.start())
                    .endTokenPosition(match.end()).surfacePhrase(surface).matchPriority(Integer.MAX_VALUE).build();
        }).toList();
    }
}
