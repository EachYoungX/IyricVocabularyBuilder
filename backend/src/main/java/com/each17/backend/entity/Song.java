package com.each17.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data // Lombok 注解，自动生成 Getter, Setter, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "songs", uniqueConstraints = @UniqueConstraint(columnNames = {"title", "artist"}))
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artist;
    
    @Lob // 表示这是一个大的文本字段
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lyrics;
}