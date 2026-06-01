package dev.deriou.airesume.service;

import dev.deriou.airesume.vo.AdminApplicationVO;
import dev.deriou.airesume.vo.AdminCreditDailyVO;
import dev.deriou.airesume.vo.AdminCreditSummaryVO;
import dev.deriou.airesume.vo.AdminCreditTopUserVO;
import dev.deriou.airesume.vo.AdminJobVO;
import dev.deriou.airesume.vo.AdminLlmDailyVO;
import dev.deriou.airesume.vo.AdminLlmOperationVO;
import dev.deriou.airesume.vo.AdminLlmSummaryVO;
import dev.deriou.airesume.vo.AdminOverviewVO;
import dev.deriou.airesume.vo.PageVO;
import java.util.List;

public interface AdminDashboardService {

    AdminOverviewVO overview();

    AdminLlmSummaryVO llmSummary();

    List<AdminLlmDailyVO> llmDaily();

    List<AdminLlmOperationVO> llmOperations();

    AdminCreditSummaryVO creditSummary();

    List<AdminCreditDailyVO> creditDaily();

    List<AdminCreditTopUserVO> creditTopUsers(int limit);

    PageVO<AdminJobVO> jobs(long page, long size, String status, String keyword);

    PageVO<AdminApplicationVO> applications(long page, long size, String status);
}
