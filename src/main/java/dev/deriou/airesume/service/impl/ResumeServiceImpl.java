package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.entity.Resume;
import dev.deriou.airesume.mapper.ResumeMapper;
import dev.deriou.airesume.service.ResumeService;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;

    public ResumeServiceImpl(ResumeMapper resumeMapper) {
        this.resumeMapper = resumeMapper;
    }

    @Override
    @Transactional
    public ResumeVO create(ResumeCreateRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = new Resume();
        resume.setUserId(user.userId());
        resume.setTitle(request.title().trim());
        resume.setContentMd(request.contentMd().trim());
        resumeMapper.insert(resume);
        return toVO(resumeMapper.selectById(resume.getId()));
    }

    @Override
    public PageVO<ResumeVO> listMine(long page, long size) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        Page<Resume> result = resumeMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, user.userId())
                        .orderByDesc(Resume::getUpdatedAt)
                        .orderByDesc(Resume::getId)
        );
        List<ResumeVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    public ResumeVO getMine(Long id) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireResume(id);
        ensureOwner(resume, user.userId());
        return toVO(resume);
    }

    @Override
    @Transactional
    public ResumeVO update(Long id, ResumeUpdateRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireResume(id);
        ensureOwner(resume, user.userId());
        resume.setTitle(request.title().trim());
        resume.setContentMd(request.contentMd().trim());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return toVO(resumeMapper.selectById(resume.getId()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireResume(id);
        ensureOwner(resume, user.userId());
        resumeMapper.deleteById(id);
    }

    private Resume requireResume(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "resume not found");
        }
        return resume;
    }

    private void ensureOwner(Resume resume, Long userId) {
        if (!userId.equals(resume.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "resume does not belong to current user");
        }
    }

    private ResumeVO toVO(Resume resume) {
        return new ResumeVO(
                resume.getId(),
                resume.getUserId(),
                resume.getTitle(),
                resume.getContentMd(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
