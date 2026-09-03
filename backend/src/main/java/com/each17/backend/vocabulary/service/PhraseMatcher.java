package com.each17.backend.vocabulary.service;

import com.each17.backend.dictionary.model.PhraseAnchor;
import com.each17.backend.dictionary.model.PhraseEntry;
import com.each17.backend.dictionary.model.PhrasePatternToken;
import com.each17.backend.dictionary.service.PhraseAnchorRepository;
import com.each17.backend.dictionary.service.PhrasePatternRepository;
import com.each17.backend.dictionary.service.PhraseRepository;
import com.each17.backend.dto.PhraseMatchDto;
import com.each17.backend.lyric.entity.LyricToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhraseMatcher {
    private static final int MAX_BACKTRACK_BRANCHES = 200;

    private final PhraseAnchorRepository anchorRepository;
    private final PhraseRepository phraseRepository;
    private final PhrasePatternRepository patternRepository;
    private final List<SlotValidator> slotValidators;

    public PhraseMatcher(PhraseAnchorRepository anchorRepository, PhraseRepository phraseRepository,
                          PhrasePatternRepository patternRepository) {
        this(anchorRepository, phraseRepository, patternRepository, List.of(
                new PossessiveSlotValidator(), new ReflexiveSlotValidator(),
                new PronounSlotValidator(), new GerundSlotValidator()
        ));
    }

    @Autowired
    public PhraseMatcher(PhraseAnchorRepository anchorRepository, PhraseRepository phraseRepository,
                          PhrasePatternRepository patternRepository, List<SlotValidator> slotValidators) {
        this.anchorRepository = anchorRepository;
        this.phraseRepository = phraseRepository;
        this.patternRepository = patternRepository;
        this.slotValidators = slotValidators;
    }

    public List<PhraseMatchDto> findMatches(List<LyricToken> tokens) {
        return findMatches(tokens, null);
    }

    public List<PhraseMatchDto> findMatches(List<LyricToken> tokens, Integer selectedPosition) {
        if (tokens == null || tokens.isEmpty()) return List.of();
        Map<Long, PhraseAnchor> candidates = new LinkedHashMap<>();
        Map<String, List<PhraseAnchor>> anchorCache = new HashMap<>();
        for (int position = 0; position < tokens.size(); position++) {
            LyricToken token = tokens.get(position);
            String key = String.join("\u0000", safe(token.getNormalizedForm()), safe(token.getLemma()), safe(token.getSurfaceForm()));
            List<PhraseAnchor> anchors = anchorCache.computeIfAbsent(key,
                    ignored -> anchorRepository.findByToken(token.getNormalizedForm(), token.getLemma(), token.getSurfaceForm()));
            anchors.forEach(anchor -> candidates.putIfAbsent(anchor.phraseId(), anchor));
        }

        List<MatchCandidate> matches = new ArrayList<>();
        for (Map.Entry<Long, PhraseAnchor> candidate : candidates.entrySet()) {
            Optional<PhraseEntry> phrase = phraseRepository.findById(candidate.getKey());
            if (phrase.isEmpty()) continue;
            List<PhrasePatternToken> pattern = patternRepository.findByPhraseId(candidate.getKey());
            if (pattern.isEmpty()) continue;
            PhraseEntry entry = phrase.get();
            int minStart = selectedPosition == null ? 0 : Math.max(0, selectedPosition - entry.tokenCountMax());
            int maxStart = selectedPosition == null ? tokens.size() - 1 : Math.min(selectedPosition, tokens.size() - 1);
            for (int start = minStart; start <= maxStart; start++) {
                if (start + entry.tokenCountMin() > tokens.size()) continue;
                List<Integer> ends = new ArrayList<>();
                matchPattern(pattern, tokens, 0, start, 0, ends);
                for (Integer endExclusive : ends) {
                    int end = endExclusive - 1;
                    if (end < start || end >= tokens.size()) continue;
                    if (selectedPosition != null && (selectedPosition < start || selectedPosition > end)) continue;
                    if (!containsAnchor(tokens, start, endExclusive, candidate.getValue())) continue;
                    matches.add(toCandidate(entry, pattern, start, end, tokens));
                }
            }
        }
        return removeContainedDuplicates(matches);
    }

    private void matchPattern(List<PhrasePatternToken> pattern, List<LyricToken> tokens,
                              int patternIndex, int tokenIndex, int branches, List<Integer> ends) {
        if (ends.size() >= MAX_BACKTRACK_BRANCHES || branches >= MAX_BACKTRACK_BRANCHES) return;
        if (patternIndex == pattern.size()) {
            ends.add(tokenIndex);
            return;
        }
        PhrasePatternToken element = pattern.get(patternIndex);
        if (element.isLiteral()) {
            if (tokenIndex < tokens.size() && matchesLiteral(element, tokens.get(tokenIndex))) {
                matchPattern(pattern, tokens, patternIndex + 1, tokenIndex + 1, branches, ends);
            }
            return;
        }
        int min = Math.max(1, element.minTokens());
        int max = Math.max(min, element.maxTokens());
        int remainingMin = minimumTokens(pattern, patternIndex + 1);
        int upper = Math.min(max, tokens.size() - tokenIndex - remainingMin);
        for (int length = min; length <= upper; length++) {
            if (!isValidSlot(element, tokens, tokenIndex, length)) continue;
            matchPattern(pattern, tokens, patternIndex + 1, tokenIndex + length,
                    branches + 1, ends);
        }
    }

    private boolean isValidSlot(PhrasePatternToken element, List<LyricToken> tokens, int start, int length) {
        if ("GAP".equalsIgnoreCase(element.tokenType()) || element.slotHint() == null) return true;
        List<LyricToken> span = tokens.subList(start, start + length);
        return slotValidators.stream()
                .filter(validator -> validator.supports(element.slotHint()))
                .findFirst()
                .map(validator -> validator.isValid(span))
                .orElse(true);
    }

    private int minimumTokens(List<PhrasePatternToken> pattern, int from) {
        int total = 0;
        for (int i = from; i < pattern.size(); i++) {
            PhrasePatternToken element = pattern.get(i);
            total += element.isLiteral() ? 1 : Math.max(1, element.minTokens());
        }
        return total;
    }

    private boolean matchesLiteral(PhrasePatternToken element, LyricToken token) {
        String expected = safe(element.matchValue()).toLowerCase();
        String actual;
        actual = switch (safe(element.matchType()).toUpperCase()) {
            case "LEMMA" -> token.getLemma();
            case "SURFACE" -> token.getSurfaceForm();
            default -> token.getNormalizedForm();
        };
        return safe(actual).equalsIgnoreCase(expected);
    }

    private boolean containsAnchor(List<LyricToken> tokens, int start, int endExclusive, PhraseAnchor anchor) {
        for (int position = start; position < endExclusive; position++) {
            LyricToken token = tokens.get(position);
            String actual = switch (safe(anchor.anchorType()).toUpperCase()) {
                case "LEMMA" -> token.getLemma();
                case "SURFACE" -> token.getSurfaceForm();
                default -> token.getNormalizedForm();
            };
            if (safe(actual).equalsIgnoreCase(anchor.anchorValue())) return true;
        }
        return false;
    }

    private MatchCandidate toCandidate(PhraseEntry entry, List<PhrasePatternToken> pattern,
                                        int start, int end, List<LyricToken> tokens) {
        PhraseMatchDto dto = PhraseMatchDto.builder()
                .phraseId(entry.id())
                .sourcePattern(entry.sourcePattern())
                .canonicalPattern(entry.canonicalPattern())
                .definitionEn(entry.definitionEn())
                .definitionZh(entry.definitionZh())
                .usageNoteZh(entry.usageNoteZh())
                .phraseType(entry.phraseType())
                .source(entry.source())
                .startTokenPosition(start)
                .endTokenPosition(end)
                .surfacePhrase(tokens.subList(start, end + 1).stream()
                        .map(LyricToken::getSurfaceForm).collect(Collectors.joining(" ")))
                .matchPriority(entry.matchPriority())
                .build();
        int literalCount = (int) pattern.stream().filter(PhrasePatternToken::isLiteral).count();
        int hardSlotCount = (int) pattern.stream()
                .filter(element -> isHardSlot(element.slotHint()))
                .count();
        int gapCount = (int) pattern.stream()
                .filter(element -> "GAP".equalsIgnoreCase(safe(element.tokenType())))
                .count();
        int specificity = literalCount * 3 + hardSlotCount * 2
                + (int) pattern.stream().filter(element -> !element.isLiteral()
                && !"GAP".equalsIgnoreCase(safe(element.tokenType()))).count();
        return new MatchCandidate(dto, specificity, literalCount, hardSlotCount, gapCount);
    }

    private boolean isHardSlot(String slotHint) {
        return "POSSESSIVE".equalsIgnoreCase(safe(slotHint))
                || "REFLEXIVE".equalsIgnoreCase(safe(slotHint))
                || "PRONOUN".equalsIgnoreCase(safe(slotHint))
                || "GERUND".equalsIgnoreCase(safe(slotHint));
    }

    private List<PhraseMatchDto> removeContainedDuplicates(List<MatchCandidate> matches) {
        Map<String, MatchCandidate> deduplicated = matches.stream()
                .collect(Collectors.toMap(match -> match.dto().getPhraseId() + ":"
                                + match.dto().getStartTokenPosition() + ":" + match.dto().getEndTokenPosition(),
                        match -> match, this::preferCandidate, LinkedHashMap::new));
        return deduplicated.values().stream()
                .filter(match -> matches.stream().noneMatch(other ->
                        !other.dto().getPhraseId().equals(match.dto().getPhraseId())
                                && other.dto().getStartTokenPosition() <= match.dto().getStartTokenPosition()
                                && other.dto().getEndTokenPosition() >= match.dto().getEndTokenPosition()
                                && spanLength(other) > spanLength(match)))
                .sorted(Comparator.comparingInt((MatchCandidate match) -> match.dto().getStartTokenPosition())
                        .thenComparing(Comparator.comparingInt(this::spanLength).reversed())
                        .thenComparing(Comparator.comparingInt(MatchCandidate::specificity).reversed())
                        .thenComparing(Comparator.comparingInt((MatchCandidate match) -> match.dto().getMatchPriority()).reversed())
                        .thenComparing(Comparator.comparingInt(MatchCandidate::literalCount).reversed())
                        .thenComparingInt(MatchCandidate::gapCount))
                .map(MatchCandidate::dto)
                .toList();
    }

    private MatchCandidate preferCandidate(MatchCandidate left, MatchCandidate right) {
        return candidateComparator().compare(left, right) >= 0 ? left : right;
    }

    private Comparator<MatchCandidate> candidateComparator() {
        return Comparator.comparingInt(MatchCandidate::specificity)
                .thenComparingInt(this::spanLength)
                .thenComparingInt(match -> match.dto().getMatchPriority())
                .thenComparingInt(MatchCandidate::literalCount)
                .thenComparing(Comparator.comparingInt(MatchCandidate::gapCount).reversed());
    }

    private int spanLength(MatchCandidate match) {
        return match.dto().getEndTokenPosition() - match.dto().getStartTokenPosition();
    }

    private record MatchCandidate(PhraseMatchDto dto, int specificity, int literalCount,
                                  int hardSlotCount, int gapCount) {}

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
