package com.youtube.research.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}