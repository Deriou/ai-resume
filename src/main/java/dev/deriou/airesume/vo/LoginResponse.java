package dev.deriou.airesume.vo;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String role,
        String nickName,
        Integer creditBalance
) {
}
