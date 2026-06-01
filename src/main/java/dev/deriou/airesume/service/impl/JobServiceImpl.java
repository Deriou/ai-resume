package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.JobCreateRequest;
import dev.deriou.airesume.dto.JobUpdateRequest;
import dev.deriou.airesume.entity.Job;
import dev.deriou.airesume.mapper.JobMapper;
import dev.deriou.airesume.service.HotDataService;
import dev.deriou.airesume.service.JobService;
import dev.deriou.airesume.vo.JobVO;
import dev.deriou.airesume.vo.PageVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobServiceImpl implements JobService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    private final JobMapper jobMapper;
    private final HotDataService hotDataService;

    public JobServiceImpl(JobMapper jobMapper, HotDataService hotDataService) {
        this.jobMapper = jobMapper;
        this.hotDataService = hotDataService;
    }

    @Override
    @Transactional
    public JobVO create(JobCreateRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        Job job = new Job();
        job.setEnterpriseId(user.userId());
        job.setTitle(request.title().trim());
        job.setJdContent(request.jdContent().trim());
        job.setTechStack(trimToNull(request.techStack()));
        job.setLocation(trimToNull(request.location()));
        job.setStatus(STATUS_OPEN);
        jobMapper.insert(job);
        hotDataService.evictHotCaches();
        return toVO(jobMapper.selectById(job.getId()));
    }

    @Override
    public PageVO<JobVO> listOpen(long page, long size) {
        LoginUserSupport.currentUser();
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        Page<Job> result = jobMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<Job>()
                        .eq(Job::getStatus, STATUS_OPEN)
                        .orderByDesc(Job::getCreatedAt)
                        .orderByDesc(Job::getId)
        );
        List<JobVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    public PageVO<JobVO> listMine(long page, long size) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        Page<Job> result = jobMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<Job>()
                        .eq(Job::getEnterpriseId, user.userId())
                        .orderByDesc(Job::getUpdatedAt)
                        .orderByDesc(Job::getId)
        );
        List<JobVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    public JobVO getVisible(Long id) {
        LoginUser user = LoginUserSupport.currentUser();
        Job job = requireJob(id);
        if (STATUS_OPEN.equals(job.getStatus())) {
            return toVO(job);
        }
        if (LoginUserSupport.ROLE_ENTERPRISE.equals(user.role()) && user.userId().equals(job.getEnterpriseId())) {
            return toVO(job);
        }
        throw new BizException(ResultCode.FORBIDDEN, "job is not visible");
    }

    @Override
    @Transactional
    public JobVO update(Long id, JobUpdateRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        Job job = requireJob(id);
        ensureOwner(job, user.userId());
        job.setTitle(request.title().trim());
        job.setJdContent(request.jdContent().trim());
        job.setTechStack(trimToNull(request.techStack()));
        job.setLocation(trimToNull(request.location()));
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        hotDataService.evictHotCaches();
        return toVO(jobMapper.selectById(job.getId()));
    }

    @Override
    @Transactional
    public JobVO close(Long id) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_ENTERPRISE);
        Job job = requireJob(id);
        ensureOwner(job, user.userId());
        job.setStatus(STATUS_CLOSED);
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        hotDataService.evictHotCaches();
        return toVO(jobMapper.selectById(job.getId()));
    }

    private Job requireJob(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "job not found");
        }
        return job;
    }

    private void ensureOwner(Job job, Long enterpriseId) {
        if (!enterpriseId.equals(job.getEnterpriseId())) {
            throw new BizException(ResultCode.FORBIDDEN, "job does not belong to current enterprise");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private JobVO toVO(Job job) {
        return new JobVO(
                job.getId(),
                job.getEnterpriseId(),
                job.getTitle(),
                job.getJdContent(),
                job.getTechStack(),
                job.getLocation(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
