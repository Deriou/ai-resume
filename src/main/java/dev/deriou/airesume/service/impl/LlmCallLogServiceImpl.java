package dev.deriou.airesume.service.impl;

import dev.deriou.airesume.entity.LlmCallLog;
import dev.deriou.airesume.llm.LlmResult;
import dev.deriou.airesume.mapper.LlmCallLogMapper;
import dev.deriou.airesume.service.LlmCallLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LlmCallLogServiceImpl implements LlmCallLogService {

    private static final String PROVIDER_DEEPSEEK = "DEEPSEEK";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final int P3_CREDIT_COST = 0;

    private final LlmCallLogMapper llmCallLogMapper;

    public LlmCallLogServiceImpl(LlmCallLogMapper llmCallLogMapper) {
        this.llmCallLogMapper = llmCallLogMapper;
    }

    @Override
    @Transactional
    public Long recordSuccess(Long userId, String operation, LlmResult result) {
        LlmCallLog log = baseLog(userId, operation, result.model(), result);
        log.setStatus(STATUS_SUCCESS);
        llmCallLogMapper.insert(log);
        return log.getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId, String operation, String fallbackModel, LlmResult result, Exception ex) {
        String model = result != null ? result.model() : fallbackModel;
        LlmCallLog log = baseLog(userId, operation, model, result);
        log.setStatus(STATUS_FAILED);
        log.setErrorMessage(truncateError(ex));
        llmCallLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void updateCreditCost(Long logId, int creditCost) {
        LlmCallLog log = new LlmCallLog();
        log.setId(logId);
        log.setCreditCost(creditCost);
        llmCallLogMapper.updateById(log);
    }

    private LlmCallLog baseLog(Long userId, String operation, String model, LlmResult result) {
        LlmCallLog log = new LlmCallLog();
        log.setUserId(userId);
        log.setOperation(operation);
        log.setProvider(PROVIDER_DEEPSEEK);
        log.setModel(StringUtils.hasText(model) ? model : "unknown");
        log.setPromptTokens(result != null ? result.promptTokens() : 0);
        log.setCompletionTokens(result != null ? result.completionTokens() : 0);
        log.setTotalTokens(result != null ? result.totalTokens() : 0);
        log.setCreditCost(P3_CREDIT_COST);
        log.setLatencyMs(result != null ? result.latencyMs() : 0);
        return log;
    }

    private String truncateError(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
