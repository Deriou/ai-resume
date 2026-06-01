package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record AdminApplicationVO(
        Long id,
        Long userId,
        String username,
        String nickName,
        Long resumeId,
        String resumeTitle,
        Long jobId,
        String jobTitle,
        Long enterpriseId,
        String enterpriseName,
        String status,
        String remark,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
