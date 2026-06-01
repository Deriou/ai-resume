package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeVO;

public interface ResumeService {

    ResumeVO create(ResumeCreateRequest request);

    PageVO<ResumeVO> listMine(long page, long size);

    ResumeVO getMine(Long id);

    ResumeVO update(Long id, ResumeUpdateRequest request);

    void delete(Long id);
}
