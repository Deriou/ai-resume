package dev.deriou.airesume.vo;

public record AdminJobSummaryVO(
        long jobCount,
        long openJobCount,
        long closedJobCount,
        long applicationCount,
        long noApplicationJobCount,
        double avgApplicationsPerJob
) {
}
