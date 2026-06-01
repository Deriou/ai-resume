package dev.deriou.airesume.service;

import dev.deriou.airesume.vo.HotCompanyVO;
import dev.deriou.airesume.vo.HotJobVO;
import java.util.List;

public interface HotDataService {

    List<HotJobVO> listHotJobs();

    List<HotCompanyVO> listHotCompanies();

    void refreshAll();

    void evictHotCaches();
}
