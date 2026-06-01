package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record AdminJobVO(
        Long id,
        Long enterpriseId,
        String enterpriseName,
        String title,
        String techStack,
        String location,
        String status,
        long applicationCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
