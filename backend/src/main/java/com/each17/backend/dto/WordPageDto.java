package com.each17.backend.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordPageDto {
    private List<String> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
    private Integer size;
}