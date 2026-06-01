package dev.deriou.airesume.vo;

public record AdminLlmOperationVO(
        String operation,
        long callCount,
        long totalTokens,
        long creditCost,
        long avgLatencyMs
) {
}
