package com.each17.backend.lyric.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CreditLineClassifier {
    private static final String ROLE = "(?:producer|co-producer|executive producer|writer|written by|lyrics|lyrics by|lyricist|composer|composed by|performed by|performer|vocals?|singer|guitar(?:ist)?|bass(?:ist)?|drums?|drummer|piano|keyboard|strings?|(?:\\d+(?:st|nd|rd|th)\\s+)?(?:violin|viola|cello|contrabass)|recording\\s*&\\s*mixing\\s+engineer|(?:recording|mixing)(?:\\s+assistant)?\\s+engineer|mix engineer|mixed by|mastering engineer|mastered by|recording studio|mixing studio|engineer|arranged by|arranger|featuring|feat\\.)";
    private static final String CHINESE_ROLE = "(?:词|曲|作词|作曲|编曲|制作|制作人|监制|混音|混音师|母带|演唱|演奏|歌手|乐器)";
    private static final Pattern ROLE_WITH_SEPARATOR = Pattern.compile(
            "^\\s*(?:" + ROLE + "|" + CHINESE_ROLE + ")\\s*(?:[:：-])\\s*\\S.+$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ROLE_BY = Pattern.compile(
            "^\\s*(?:produced by|written by|lyrics by|composed by|performed by|mixed by|mastered by|arranged by)\\s+\\S.+$",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isCredit(String text) {
        return text != null && (ROLE_WITH_SEPARATOR.matcher(text).matches() || ROLE_BY.matcher(text).matches());
    }
}
