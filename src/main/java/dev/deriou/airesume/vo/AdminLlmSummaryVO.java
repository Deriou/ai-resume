package dev.deriou.airesume.vo;

public record AdminLlmSummaryVO(
        int days,
        long callCount,
        long successCount,
        long failureCount,
        double failureRate,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long avgLatencyMs,
        long creditCost
) {
}
