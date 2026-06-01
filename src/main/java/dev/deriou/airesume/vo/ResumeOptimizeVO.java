package dev.deriou.airesume.vo;

import java.util.List;

public record ResumeOptimizeVO(
        String summary,
        List<String> optimizedBullets,
        List<String> rewriteSuggestions,
        String llmModel,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs
) {
}
