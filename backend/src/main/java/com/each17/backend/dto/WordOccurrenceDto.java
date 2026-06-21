package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordOccurrenceDto {
    private String songTitle;
    private String lyricLine;
}