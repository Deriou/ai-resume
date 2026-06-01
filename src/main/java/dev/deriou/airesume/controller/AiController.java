package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.dto.JobMatchRequest;
import dev.deriou.airesume.dto.ResumeOptimizeRequest;
import dev.deriou.airesume.dto.ResumeScoreRequest;
import dev.deriou.airesume.service.AiResumeService;
import dev.deriou.airesume.vo.JobMatchVO;
import dev.deriou.airesume.vo.ResumeOptimizeVO;
import dev.deriou.airesume.vo.ResumeScoreVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiResumeService aiResumeService;

    public AiController(AiResumeService aiResumeService) {
        this.aiResumeService = aiResumeService;
    }

    @PostMapping("/resumes/{resumeId}/score")
    public ApiResponse<ResumeScoreVO> scoreResume(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeScoreRequest request
    ) {
        return ApiResponse.success(aiResumeService.scoreResume(resumeId, request));
    }

    @PostMapping("/resumes/{resumeId}/optimize")
    public ApiResponse<ResumeOptimizeVO> optimizeResume(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeOptimizeRequest request
    ) {
        return ApiResponse.success(aiResumeService.optimizeResume(resumeId, request));
    }

    @PostMapping("/jobs/{jobId}/match")
    public ApiResponse<JobMatchVO> matchJob(
            @PathVariable Long jobId,
            @Valid @RequestBody JobMatchRequest request
    ) {
        return ApiResponse.success(aiResumeService.matchJob(jobId, request));
    }
}
