package dev.deriou.airesume.vo;

public record AdminApplicationSummaryVO(
        long applicationCount,
        long pendingCount,
        long viewedCount,
        long acceptedCount,
        long rejectedCount,
        double reviewedRate
) {
}
