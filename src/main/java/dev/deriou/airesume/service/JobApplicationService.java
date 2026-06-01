package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.ApplicationCreateRequest;
import dev.deriou.airesume.dto.ApplicationReviewRequest;
import dev.deriou.airesume.vo.ApplicationVO;
import dev.deriou.airesume.vo.PageVO;

public interface JobApplicationService {

    ApplicationVO create(ApplicationCreateRequest request);

    PageVO<ApplicationVO> listMine(long page, long size);

    PageVO<ApplicationVO> listReceived(long page, long size);

    ApplicationVO review(Long id, ApplicationReviewRequest request);
}
