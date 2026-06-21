package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryEntryDto {
    private String word;
    private String phonetic;
    private String definition;
    private String translation;
    private String pos;
    private Integer collins;
    private Integer bnc;
    private Integer frq;
    private String forms;
}