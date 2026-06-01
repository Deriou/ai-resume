package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.service.HotDataService;
import dev.deriou.airesume.vo.HotCompanyVO;
import dev.deriou.airesume.vo.HotJobVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hot")
public class HotController {

    private final HotDataService hotDataService;

    public HotController(HotDataService hotDataService) {
        this.hotDataService = hotDataService;
    }

    @GetMapping("/jobs")
    public ApiResponse<List<HotJobVO>> hotJobs() {
        return ApiResponse.success(hotDataService.listHotJobs());
    }

    @GetMapping("/companies")
    public ApiResponse<List<HotCompanyVO>> hotCompanies() {
        return ApiResponse.success(hotDataService.listHotCompanies());
    }
}
