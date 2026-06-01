package dev.deriou.airesume.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeScoreVO(
        Long id,
        Long resumeId,
        String targetDirection,
        int overallScore,
        List<ScoreDimensionVO> dimensions,
        List<String> suggestions,
        String llmModel,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        LocalDateTime createdAt
) {
}
