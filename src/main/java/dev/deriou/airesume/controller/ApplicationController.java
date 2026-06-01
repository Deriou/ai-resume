package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.dto.ApplicationCreateRequest;
import dev.deriou.airesume.dto.ApplicationReviewRequest;
import dev.deriou.airesume.service.JobApplicationService;
import dev.deriou.airesume.vo.ApplicationVO;
import dev.deriou.airesume.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final JobApplicationService applicationService;

    public ApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApiResponse<ApplicationVO> create(@Valid @RequestBody ApplicationCreateRequest request) {
        return ApiResponse.success(applicationService.create(request));
    }

    @GetMapping("/my")
    public ApiResponse<PageVO<ApplicationVO>> listMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(applicationService.listMine(page, size));
    }

    @GetMapping("/received")
    public ApiResponse<PageVO<ApplicationVO>> listReceived(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(applicationService.listReceived(page, size));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ApplicationVO> review(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationReviewRequest request
    ) {
        return ApiResponse.success(applicationService.review(id, request));
    }
}
