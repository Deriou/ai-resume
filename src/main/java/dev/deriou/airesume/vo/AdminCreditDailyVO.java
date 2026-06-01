package dev.deriou.airesume.vo;

import java.time.LocalDate;

public record AdminCreditDailyVO(
        LocalDate date,
        long grantedCredits,
        long consumedCredits,
        long checkInCredits,
        long adminGrantCredits,
        long aiConsumedCredits
) {
}
