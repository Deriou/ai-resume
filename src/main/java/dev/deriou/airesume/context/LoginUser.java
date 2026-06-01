package dev.deriou.airesume.context;

public record LoginUser(
        Long userId,
        String username,
        String role,
        String nickName,
        Integer creditBalance,
        String token
) {
}
