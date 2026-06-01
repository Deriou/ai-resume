package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.AdminCreditGrantRequest;
import dev.deriou.airesume.vo.AdminUserVO;
import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;

public interface AdminUserService {

    PageVO<AdminUserVO> users(long page, long size, String role, String status, String keyword);

    CreditTransactionVO grantCredit(Long userId, AdminCreditGrantRequest request);
}
