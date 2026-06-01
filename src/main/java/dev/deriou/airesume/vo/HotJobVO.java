package dev.deriou.airesume.vo;

public record HotJobVO(
        Long jobId,
        String title,
        Long enterpriseId,
        String location,
        String techStack,
        Long applicationCount
) {
}
