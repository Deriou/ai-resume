package dev.deriou.airesume.vo;

public record AdminCreditTopUserVO(
        Long userId,
        String username,
        String nickName,
        long consumedCredits,
        long totalTokens,
        long aiCallCount
) {
}
