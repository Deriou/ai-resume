package dev.deriou.airesume.service;

import dev.deriou.airesume.vo.ResumeFileVO;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeFileService {

    ResumeFileVO upload(Long resumeId, MultipartFile file);

    List<ResumeFileVO> listMine(Long resumeId);

    DownloadFile getDownload(Long fileId);

    void delete(Long fileId);

    boolean existsByResumeId(Long resumeId);

    record DownloadFile(Resource resource, String originalName, String contentType, Long fileSize) {
    }
}
