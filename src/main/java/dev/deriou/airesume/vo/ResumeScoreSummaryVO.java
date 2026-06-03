package dev.deriou.airesume.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeScoreSummaryVO(
        Long resumeId,
        String targetDirection,
        Integer overallScore,
        List<String> suggestions,
        LocalDateTime scoredAt
) {
}
