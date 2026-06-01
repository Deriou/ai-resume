package dev.deriou.airesume.vo;

import java.time.LocalDateTime;

public record AdminUserVO(
        Long id,
        String username,
        String role,
        String nickName,
        Integer creditBalance,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
