package dev.deriou.airesume.service;

import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;
import java.time.LocalDate;

public interface CreditService {

    String TYPE_AI_RESUME_SCORE = "AI_RESUME_SCORE";
    String TYPE_AI_RESUME_OPTIMIZE = "AI_RESUME_OPTIMIZE";
    String TYPE_AI_JOB_MATCH = "AI_JOB_MATCH";
    String TYPE_CHECK_IN = "CHECK_IN";
    String TYPE_ADMIN_GRANT = "ADMIN_GRANT";
    String REF_TYPE_LLM_CALL_LOG = "LLM_CALL_LOG";
    String REF_TYPE_CHECK_IN = "CHECK_IN";

    Integer getBalance(Long userId);

    void ensureSufficient(Long userId, int amount);

    CreditTransactionVO charge(Long userId, int amount, String type, String refType, Long refId, String remark);

    CreditTransactionVO grant(Long userId, int amount, String type, String refType, Long refId, String remark);

    PageVO<CreditTransactionVO> listMyTransactions(long page, long size);

    boolean hasTransactionOnDate(Long userId, String type, LocalDate date);
}
