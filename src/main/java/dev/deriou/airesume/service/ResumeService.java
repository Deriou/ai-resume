package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeOptimizeRecordVO;
import dev.deriou.airesume.vo.ResumeScoreSummaryVO;
import dev.deriou.airesume.vo.ResumeVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeVO create(ResumeCreateRequest request);

    ResumeVO importPdf(MultipartFile file, String title);

    PageVO<ResumeVO> listMine(long page, long size);

    ResumeVO getMine(Long id);

    ResumeVO update(Long id, ResumeUpdateRequest request);

    void delete(Long id);

    List<ResumeScoreSummaryVO> listLatestScores();

    List<ResumeOptimizeRecordVO> listOptimizations(Long resumeId);
}
