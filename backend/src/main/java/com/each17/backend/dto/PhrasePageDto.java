package com.each17.backend.dto;

import com.each17.backend.dictionary.model.PhraseEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhrasePageDto {
    private List<PhraseEntry> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
    private Integer size;
}
