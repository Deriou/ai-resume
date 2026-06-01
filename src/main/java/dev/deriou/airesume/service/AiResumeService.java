package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.JobMatchRequest;
import dev.deriou.airesume.dto.ResumeOptimizeRequest;
import dev.deriou.airesume.dto.ResumeScoreRequest;
import dev.deriou.airesume.vo.JobMatchVO;
import dev.deriou.airesume.vo.ResumeOptimizeVO;
import dev.deriou.airesume.vo.ResumeScoreVO;

public interface AiResumeService {

    ResumeScoreVO scoreResume(Long resumeId, ResumeScoreRequest request);

    ResumeOptimizeVO optimizeResume(Long resumeId, ResumeOptimizeRequest request);

    JobMatchVO matchJob(Long jobId, JobMatchRequest request);
}
