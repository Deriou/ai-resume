package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record JobVO(
        Long id,
        Long enterpriseId,
        String title,
        String jdContent,
        String techStack,
        String location,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
