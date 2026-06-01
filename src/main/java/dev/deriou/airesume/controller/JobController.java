package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.dto.JobCreateRequest;
import dev.deriou.airesume.dto.JobUpdateRequest;
import dev.deriou.airesume.service.JobService;
import dev.deriou.airesume.vo.JobVO;
import dev.deriou.airesume.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ApiResponse<JobVO> create(@Valid @RequestBody JobCreateRequest request) {
        return ApiResponse.success(jobService.create(request));
    }

    @GetMapping
    public ApiResponse<PageVO<JobVO>> listOpen(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(jobService.listOpen(page, size));
    }

    @GetMapping("/mine")
    public ApiResponse<PageVO<JobVO>> listMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(jobService.listMine(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobVO> getVisible(@PathVariable Long id) {
        return ApiResponse.success(jobService.getVisible(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobVO> update(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request
    ) {
        return ApiResponse.success(jobService.update(id, request));
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<JobVO> close(@PathVariable Long id) {
        return ApiResponse.success(jobService.close(id));
    }
}
