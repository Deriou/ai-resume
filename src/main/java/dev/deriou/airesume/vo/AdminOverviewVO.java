package dev.deriou.airesume.vo;

public record AdminOverviewVO(
        long totalUsers,
        long userCount,
        long enterpriseCount,
        long adminCount,
        long resumeCount,
        long jobCount,
        long openJobCount,
        long applicationCount,
        long todayAiCalls,
        long todayTokens,
        long todayCreditCost,
        long todayAvgLatencyMs,
        long todayAiFailures
) {
}
