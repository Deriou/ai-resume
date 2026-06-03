package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record ApplicationVO(
        Long id,
        Long userId,
        Long resumeId,
        String resumeTitle,
        String resumeContentMd,
        Long jobId,
        String jobTitle,
        String status,
        String remark,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
