package dev.deriou.airesume.vo;

public record AdminCreditSummaryVO(
        int days,
        long grantedCredits,
        long consumedCredits,
        long checkInCredits,
        long adminGrantCredits,
        long resumeScoreCredits,
        long resumeOptimizeCredits,
        long jobMatchCredits
) {
}
