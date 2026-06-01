package dev.deriou.airesume.vo;

public record HotCompanyVO(
        Long enterpriseId,
        String enterpriseName,
        Long openJobCount,
        Long applicationCount
) {
}
