package com.each17.backend.dictionary.model;

public record PhrasePatternToken(
        Long phraseId,
        int patternPosition,
        String tokenType,
        String matchType,
        String matchValue,
        String slotHint,
        int minTokens,
        int maxTokens
) {
    public boolean isLiteral() {
        return "LITERAL".equalsIgnoreCase(tokenType);
    }
}
