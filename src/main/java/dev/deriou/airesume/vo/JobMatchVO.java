package dev.deriou.airesume.vo;

import java.time.LocalDateTime;
import java.util.List;

public record JobMatchVO(
        Long id,
        Long resumeId,
        Long jobId,
        int matchScore,
        List<String> strengths,
        List<String> gaps,
        List<String> suggestions,
        LocalDateTime createdAt
) {
}
