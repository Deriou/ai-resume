package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.ApplicationCreateRequest;
import dev.deriou.airesume.dto.ApplicationReviewRequest;
import dev.deriou.airesume.entity.Job;
import dev.deriou.airesume.entity.JobApplication;
import dev.deriou.airesume.entity.Resume;
import dev.deriou.airesume.mapper.JobApplicationMapper;
import dev.deriou.airesume.mapper.JobMapper;
import dev.deriou.airesume.mapper.ResumeMapper;
import dev.deriou.airesume.service.JobApplicationService;
import dev.deriou.airesume.vo.ApplicationVO;
import dev.deriou.airesume.vo.PageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VIEWED = "VIEWED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final JobApplicationMapper applicationMapper;
    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;

    public JobApplicationServiceImpl(
            JobApplicationMapper applicationMapper,
            ResumeMapper resumeMapper,
            JobMapper jobMapper
    ) {
        this.applicationMapper = applicationMapper;
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
    }

    @Override
    @Transactional
    public ApplicationVO create(ApplicationCreateRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireResume(request.resumeId());
        if (!user.userId().equals(resume.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "resume does not belong to current user");
        }

        Job job = requireJob(request.jobId());
        if (!JobServiceImpl.STATUS_OPEN.equals(job.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "job is closed");
        }

        boolean exists = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, user.userId())
                .eq(JobApplication::getJobId, job.getId())) > 0;
        if (exists) {
            throw new BizException(ResultCode.BIZ_ERROR, "application already exists");
        }

        JobApplication application = new JobApplication();
        application.setUserId(user.userId());
        application.setResumeId(resume.getId());
        application.setJobId(job.getId());
        application.setStatus(STATUS_PENDING);
        application.setRemark(trimToNull(request.remark()));
        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "application already exists", ex);
        }
        return toVO(applicationMapper.selectById(application.getId()), resume, job);
    }

    @Override
    public PageVO<ApplicationVO> listMine(long page, long size) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        Page<JobApplication> result = applicationMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getUserId, user.userId())
                        .orderByDesc(JobApplication::getUpdatedAt)
                        .orderByDesc(JobApplication::getId)
        );
        List<ApplicationVO> records = toVOList(result.getRecords());
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    public PageVO<ApplicationVO> listReceived(long page, long size) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getEnterpriseId, user.userId()));
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        if (jobIds.isEmpty()) {
            return PageVO.of(List.of(), current, pageSize, 0, 0);
        }

        Page<JobApplication> result = applicationMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<JobApplication>()
                        .in(JobApplication::getJobId, jobIds)
                        .orderByDesc(JobApplication::getUpdatedAt)
                        .orderByDesc(JobApplication::getId)
        );
        List<ApplicationVO> records = toVOList(result.getRecords());
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    @Transactional
    public ApplicationVO review(Long id, ApplicationReviewRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        JobApplication application = requireApplication(id);
        Job job = requireJob(application.getJobId());
        if (!user.userId().equals(job.getEnterpriseId())) {
            throw new BizException(ResultCode.FORBIDDEN, "application does not belong to current enterprise");
        }

        String nextStatus = normalizeReviewStatus(request.status());
        if (STATUS_REJECTED.equals(application.getStatus()) || STATUS_ACCEPTED.equals(application.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "application has already been reviewed");
        }

        application.setStatus(nextStatus);
        application.setRemark(trimToNull(request.remark()));
        application.setReviewedBy(user.userId());
        application.setReviewedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(application);

        Resume resume = resumeMapper.selectById(application.getResumeId());
        return toVO(applicationMapper.selectById(application.getId()), resume, job);
    }

    private JobApplication requireApplication(Long id) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "application not found");
        }
        return application;
    }

    private Resume requireResume(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "resume not found");
        }
        return resume;
    }

    private Job requireJob(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "job not found");
        }
        return job;
    }

    private String normalizeReviewStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_VIEWED.equals(normalized)
                && !STATUS_REJECTED.equals(normalized)
                && !STATUS_ACCEPTED.equals(normalized)) {
            throw new BizException(ResultCode.BIZ_ERROR, "status must be VIEWED, REJECTED or ACCEPTED");
        }
        return normalized;
    }

    private List<ApplicationVO> toVOList(List<JobApplication> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }
        List<Long> resumeIds = applications.stream().map(JobApplication::getResumeId).distinct().toList();
        List<Long> jobIds = applications.stream().map(JobApplication::getJobId).distinct().toList();
        Map<Long, Resume> resumes = resumeMapper.selectBatchIds(resumeIds).stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));
        Map<Long, Job> jobs = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        return applications.stream()
                .map(application -> toVO(
                        application,
                        resumes.get(application.getResumeId()),
                        jobs.get(application.getJobId())
                ))
                .toList();
    }

    private ApplicationVO toVO(JobApplication application, Resume resume, Job job) {
        return new ApplicationVO(
                application.getId(),
                application.getUserId(),
                application.getResumeId(),
                resume != null ? resume.getTitle() : null,
                application.getJobId(),
                job != null ? job.getTitle() : null,
                application.getStatus(),
                application.getRemark(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
