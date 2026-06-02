package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record ResumeFileVO(
        Long id,
        Long resumeId,
        String originalName,
        String contentType,
        Long fileSize,
        String fileExt,
        LocalDateTime createdAt
) {
}
