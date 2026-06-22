package com.each17.backend.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vocabulary")
public class Vocabulary {
    @Id
    private String word;
    
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String occurrences; // 存储 JSON 字符串
}
