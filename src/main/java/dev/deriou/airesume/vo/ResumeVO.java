package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record ResumeVO(
        Long id,
        Long userId,
        String title,
        String contentMd,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
