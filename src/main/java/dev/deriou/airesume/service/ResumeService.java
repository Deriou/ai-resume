package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeVO create(ResumeCreateRequest request);

    ResumeVO importPdf(MultipartFile file, String title);

    PageVO<ResumeVO> listMine(long page, long size);

    ResumeVO getMine(Long id);

    ResumeVO update(Long id, ResumeUpdateRequest request);

    void delete(Long id);
}
