package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongDto {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private String rawTitle;
    private String rawArtist;
    private String rawSourceContent;
    private String lyrics;
    private java.util.List<SongCreditDto> credits;
}
