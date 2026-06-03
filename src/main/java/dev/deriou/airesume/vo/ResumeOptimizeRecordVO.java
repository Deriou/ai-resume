package dev.deriou.airesume.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeOptimizeRecordVO(
        Long id,
        Long resumeId,
        String targetDirection,
        String summary,
        List<String> optimizedBullets,
        List<String> rewriteSuggestions,
        String llmModel,
        Integer totalTokens,
        Long latencyMs,
        LocalDateTime createdAt
) {
}
