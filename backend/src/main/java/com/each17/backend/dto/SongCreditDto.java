package com.each17.backend.dto;

import com.each17.backend.song.entity.SongCreditType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongCreditDto {
    private Long id;
    private SongCreditType creditType;
    private String creditLabel;
    private String creditValue;
    private Long sourceLineId;
    private Integer sortOrder;
}
