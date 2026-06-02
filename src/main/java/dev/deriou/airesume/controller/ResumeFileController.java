package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.service.ResumeFileService;
import dev.deriou.airesume.vo.ResumeFileVO;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ResumeFileController {

    private final ResumeFileService resumeFileService;

    public ResumeFileController(ResumeFileService resumeFileService) {
        this.resumeFileService = resumeFileService;
    }

    @PostMapping(value = "/resumes/{resumeId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeFileVO> upload(
            @PathVariable Long resumeId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(resumeFileService.upload(resumeId, file));
    }

    @GetMapping("/resumes/{resumeId}/files")
    public ApiResponse<List<ResumeFileVO>> listMine(@PathVariable Long resumeId) {
        return ApiResponse.success(resumeFileService.listMine(resumeId));
    }

    @GetMapping("/resume-files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        ResumeFileService.DownloadFile download = resumeFileService.getDownload(fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalName(), StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null && !download.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(download.contentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/resume-files/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        resumeFileService.delete(fileId);
        return ApiResponse.success(null);
    }
}
