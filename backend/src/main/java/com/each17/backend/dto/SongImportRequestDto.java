package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongImportRequestDto {
    private String title;
    private String artist;
    private String album;
    private String lyrics;
    private String rawSourceContent;
}
