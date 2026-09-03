package com.each17.backend.lyric.service;

import com.each17.backend.song.entity.SongCreditType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class CreditLineClassifier {
    private static final String ROLE = "(?:producer|co-producer|executive producer|writer|written by|lyrics|lyrics by|lyricist|composer|composed by|performed by|performer|vocals?|singer|guitar(?:ist)?|bass(?:ist)?|drums?|drummer|piano|keyboard|strings?|(?:\\d+(?:st|nd|rd|th)\\s+)?(?:violin|viola|cello|contrabass)|recording\\s*&\\s*mixing\\s+engineer|(?:recording|mixing)(?:\\s+assistant)?\\s+engineer|mix engineer|mixed by|mastering engineer|mastered by|recording studio|mixing studio|engineer|arranged by|arranger|featuring|feat\\.)";
    private static final String CHINESE_ROLE = "(?:词|曲|作词|作曲|编曲|制作|制作人|监制|混音|混音师|母带|演唱|演奏|歌手|乐器)";
    private static final Pattern ROLE_WITH_SEPARATOR = Pattern.compile(
            "^\\s*(?<label>" + ROLE + "|" + CHINESE_ROLE + ")\\s*(?:[:：-])\\s*(?<value>\\S.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ROLE_BY = Pattern.compile(
            "^\\s*(?<label>produced by|written by|lyrics by|composed by|performed by|mixed by|mastered by|arranged by)\\s+(?<value>\\S.+)$",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isCredit(String text) {
        return text != null && (ROLE_WITH_SEPARATOR.matcher(text).matches() || ROLE_BY.matcher(text).matches());
    }

    public Optional<ParsedCredit> parse(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        var separator = ROLE_WITH_SEPARATOR.matcher(text);
        String label;
        String value;
        if (separator.matches()) {
            label = separator.group("label").trim();
            value = separator.group("value").trim();
        } else {
            var by = ROLE_BY.matcher(text);
            if (!by.matches()) return Optional.empty();
            label = by.group("label").trim();
            value = by.group("value").trim();
        }
        if (value.isBlank()) return Optional.empty();
        return Optional.of(new ParsedCredit(toCreditType(label), label, value));
    }

    private SongCreditType toCreditType(String label) {
        String normalized = label.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.contains("lyric") || normalized.equals("lyrics") || normalized.equals("词") || normalized.equals("作词")) return SongCreditType.LYRICIST;
        if (normalized.contains("composer") || normalized.contains("composed") || normalized.equals("曲") || normalized.equals("作曲")) return SongCreditType.COMPOSER;
        if (normalized.contains("co-producer")) return SongCreditType.CO_PRODUCER;
        if (normalized.contains("executive producer")) return SongCreditType.EXECUTIVE_PRODUCER;
        if (normalized.contains("producer") || normalized.equals("制作") || normalized.equals("制作人") || normalized.equals("监制")) return SongCreditType.PRODUCER;
        if (normalized.contains("mix")) return SongCreditType.MIXING_ENGINEER;
        if (normalized.contains("master")) return SongCreditType.MASTERING_ENGINEER;
        if (normalized.contains("arrang") || normalized.equals("编曲")) return SongCreditType.ARRANGER;
        if (normalized.contains("vocal") || normalized.contains("singer") || normalized.equals("演唱") || normalized.equals("歌手")) return SongCreditType.VOCALS;
        if (normalized.contains("guitar")) return SongCreditType.GUITAR;
        if (normalized.contains("bass")) return SongCreditType.BASS;
        if (normalized.contains("drum")) return SongCreditType.DRUMS;
        if (normalized.contains("piano")) return SongCreditType.PIANO;
        if (normalized.contains("keyboard")) return SongCreditType.KEYBOARD;
        if (normalized.contains("violin")) return SongCreditType.VIOLIN;
        if (normalized.contains("perform") || normalized.equals("演奏")) return SongCreditType.PERFORMER;
        if (normalized.contains("feat")) return SongCreditType.FEATURING;
        return SongCreditType.OTHER;
    }

    public record ParsedCredit(SongCreditType creditType, String creditLabel, String creditValue) {}
}
