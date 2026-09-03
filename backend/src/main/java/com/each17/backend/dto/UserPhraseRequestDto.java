package com.each17.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPhraseRequestDto(@NotBlank String canonicalPhrase, String definition) {
}
