package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.service.ResumeService;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeOptimizeRecordVO;
import dev.deriou.airesume.vo.ResumeScoreSummaryVO;
import dev.deriou.airesume.vo.ResumeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ApiResponse<ResumeVO> create(@Valid @RequestBody ResumeCreateRequest request) {
        return ApiResponse.success(resumeService.create(request));
    }

    @PostMapping(value = "/import/pdf", consumes = "multipart/form-data")
    public ApiResponse<ResumeVO> importPdf(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title
    ) {
        return ApiResponse.success(resumeService.importPdf(file, title));
    }

    @GetMapping
    public ApiResponse<PageVO<ResumeVO>> listMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(resumeService.listMine(page, size));
    }

    @GetMapping("/scores/latest")
    public ApiResponse<List<ResumeScoreSummaryVO>> listLatestScores() {
        return ApiResponse.success(resumeService.listLatestScores());
    }

    @GetMapping("/{id}/optimizations")
    public ApiResponse<List<ResumeOptimizeRecordVO>> listOptimizations(@PathVariable Long id) {
        return ApiResponse.success(resumeService.listOptimizations(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeVO> getMine(@PathVariable Long id) {
        return ApiResponse.success(resumeService.getMine(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResumeVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ResumeUpdateRequest request
    ) {
        return ApiResponse.success(resumeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return ApiResponse.success(null);
    }
}
