package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record CreditTransactionVO(
        Long id,
        Long userId,
        Integer changeAmount,
        String type,
        Integer balanceAfter,
        String refType,
        Long refId,
        String remark,
        LocalDateTime createdAt
) {
}
