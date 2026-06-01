package dev.deriou.airesume.vo;

import java.time.LocalDate;

public record AdminLlmDailyVO(
        LocalDate date,
        long callCount,
        long successCount,
        long failureCount,
        long totalTokens,
        long creditCost,
        long avgLatencyMs
) {
}
