export type PhraseOccurrence = {
    phraseId?: number;
    songId?: number;
    songTitle: string;
    songArtist?: string | null;
    lyricLineId?: number | null;
    lineIndex?: number;
    lyricLine: string;
    startTokenPosition?: number;
    endTokenPosition?: number;
    surfacePhrase?: string;
    learningScore?: number;
};
