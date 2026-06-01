package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.service.CheckInService;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.vo.CheckInStatusVO;
import dev.deriou.airesume.vo.CreditBalanceVO;
import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService creditService;
    private final CheckInService checkInService;

    public CreditController(CreditService creditService, CheckInService checkInService) {
        this.creditService = creditService;
        this.checkInService = checkInService;
    }

    @GetMapping("/balance")
    public ApiResponse<CreditBalanceVO> balance() {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        return ApiResponse.success(new CreditBalanceVO(creditService.getBalance(user.userId())));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageVO<CreditTransactionVO>> transactions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(creditService.listMyTransactions(page, size));
    }

    @GetMapping("/check-in/today")
    public ApiResponse<CheckInStatusVO> today() {
        return ApiResponse.success(checkInService.today());
    }

    @PostMapping("/check-in")
    public ApiResponse<CheckInStatusVO> checkIn() {
        return ApiResponse.success(checkInService.checkIn());
    }
}
