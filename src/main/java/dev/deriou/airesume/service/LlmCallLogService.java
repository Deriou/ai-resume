package dev.deriou.airesume.service;

import dev.deriou.airesume.llm.LlmResult;

public interface LlmCallLogService {

    Long recordSuccess(Long userId, String operation, LlmResult result);

    void recordFailure(Long userId, String operation, String fallbackModel, LlmResult result, Exception ex);

    void updateCreditCost(Long logId, int creditCost);
}
