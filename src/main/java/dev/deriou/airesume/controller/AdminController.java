package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.dto.AdminCreditGrantRequest;
import dev.deriou.airesume.service.AdminDashboardService;
import dev.deriou.airesume.service.AdminUserService;
import dev.deriou.airesume.vo.AdminApplicationVO;
import dev.deriou.airesume.vo.AdminApplicationSummaryVO;
import dev.deriou.airesume.vo.AdminCreditDailyVO;
import dev.deriou.airesume.vo.AdminCreditSummaryVO;
import dev.deriou.airesume.vo.AdminCreditTopUserVO;
import dev.deriou.airesume.vo.AdminJobVO;
import dev.deriou.airesume.vo.AdminJobSummaryVO;
import dev.deriou.airesume.vo.AdminLlmDailyVO;
import dev.deriou.airesume.vo.AdminLlmOperationVO;
import dev.deriou.airesume.vo.AdminLlmSummaryVO;
import dev.deriou.airesume.vo.AdminOverviewVO;
import dev.deriou.airesume.vo.AdminUserVO;
import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final AdminUserService userService;

    public AdminController(AdminDashboardService dashboardService, AdminUserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewVO> overview() {
        return ApiResponse.success(dashboardService.overview());
    }

    @GetMapping("/llm/summary")
    public ApiResponse<AdminLlmSummaryVO> llmSummary() {
        return ApiResponse.success(dashboardService.llmSummary());
    }

    @GetMapping("/llm/daily")
    public ApiResponse<List<AdminLlmDailyVO>> llmDaily() {
        return ApiResponse.success(dashboardService.llmDaily());
    }

    @GetMapping("/llm/operations")
    public ApiResponse<List<AdminLlmOperationVO>> llmOperations() {
        return ApiResponse.success(dashboardService.llmOperations());
    }

    @GetMapping("/credits/summary")
    public ApiResponse<AdminCreditSummaryVO> creditSummary() {
        return ApiResponse.success(dashboardService.creditSummary());
    }

    @GetMapping("/credits/daily")
    public ApiResponse<List<AdminCreditDailyVO>> creditDaily() {
        return ApiResponse.success(dashboardService.creditDaily());
    }

    @GetMapping("/credits/top-users")
    public ApiResponse<List<AdminCreditTopUserVO>> creditTopUsers(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.success(dashboardService.creditTopUsers(limit));
    }

    @GetMapping("/users")
    public ApiResponse<PageVO<AdminUserVO>> users(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(userService.users(page, size, role, status, keyword));
    }

    @PostMapping("/users/{userId}/credits/grant")
    public ApiResponse<CreditTransactionVO> grantCredit(
            @PathVariable Long userId,
            @Valid @RequestBody AdminCreditGrantRequest request
    ) {
        return ApiResponse.success(userService.grantCredit(userId, request));
    }

    @GetMapping("/jobs")
    public ApiResponse<PageVO<AdminJobVO>> jobs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(dashboardService.jobs(page, size, status, keyword));
    }

    @GetMapping("/jobs/summary")
    public ApiResponse<AdminJobSummaryVO> jobSummary() {
        return ApiResponse.success(dashboardService.jobSummary());
    }

    @GetMapping("/applications/summary")
    public ApiResponse<AdminApplicationSummaryVO> applicationSummary() {
        return ApiResponse.success(dashboardService.applicationSummary());
    }

    @GetMapping("/applications")
    public ApiResponse<PageVO<AdminApplicationVO>> applications(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(dashboardService.applications(page, size, status));
    }
}
