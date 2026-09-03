package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongUpdateRequestDto {
    private String title;
    private String artist;
    private String album;
    private String lyrics;
}
