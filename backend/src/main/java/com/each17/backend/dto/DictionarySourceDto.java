package com.each17.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionarySourceDto {
    private String sourceName;
    private String sourceUrl;
    private String dictionaryVersion;
    private String importedAt;
    private String licenseName;
    private Boolean requiresAttribution;
    private Boolean commercialUseAllowed;
    private Boolean redistributionAllowed;
    private String attributionText;
}
