package com.each17.backend.lyric.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LearningValuePolicyTest {
    private final LearningValuePolicy policy = new LearningValuePolicy();

    @Test
    void downranksFillersAndFunctionWordsWithoutDeletingThem() {
        assertFalse(policy.recommended(policy.score("yeah", "yeah")));
        assertFalse(policy.recommended(policy.score("the", "the")));
        assertTrue(policy.recommended(policy.score("running", "run")));
    }
}
