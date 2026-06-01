package dev.deriou.airesume.vo;

import java.time.LocalDate;

public record CheckInStatusVO(
        Boolean checkedIn,
        LocalDate date,
        Integer creditReward,
        Integer balance
) {
}
