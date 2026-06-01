package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.JobCreateRequest;
import dev.deriou.airesume.dto.JobUpdateRequest;
import dev.deriou.airesume.vo.JobVO;
import dev.deriou.airesume.vo.PageVO;

public interface JobService {

    JobVO create(JobCreateRequest request);

    PageVO<JobVO> listOpen(long page, long size);

    PageVO<JobVO> listMine(long page, long size);

    JobVO getVisible(Long id);

    JobVO update(Long id, JobUpdateRequest request);

    JobVO close(Long id);
}
