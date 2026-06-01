package dev.deriou.airesume.llm;

public record LlmResult(
        String content,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs
) {
}
