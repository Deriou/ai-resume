package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.config.UploadProperties;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.entity.Job;
import dev.deriou.airesume.entity.JobApplication;
import dev.deriou.airesume.entity.Resume;
import dev.deriou.airesume.entity.ResumeFile;
import dev.deriou.airesume.mapper.JobApplicationMapper;
import dev.deriou.airesume.mapper.JobMapper;
import dev.deriou.airesume.mapper.ResumeFileMapper;
import dev.deriou.airesume.mapper.ResumeMapper;
import dev.deriou.airesume.service.ResumeFileService;
import dev.deriou.airesume.vo.ResumeFileVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeFileServiceImpl implements ResumeFileService {

    private static final Logger log = LoggerFactory.getLogger(ResumeFileServiceImpl.class);
    private static final int MAX_FILE_COUNT_PER_RESUME = 3;
    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final DateTimeFormatter STORED_NAME_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream"
    );

    private final ResumeFileMapper resumeFileMapper;
    private final ResumeMapper resumeMapper;
    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final UploadProperties uploadProperties;

    public ResumeFileServiceImpl(
            ResumeFileMapper resumeFileMapper,
            ResumeMapper resumeMapper,
            JobApplicationMapper applicationMapper,
            JobMapper jobMapper,
            UploadProperties uploadProperties
    ) {
        this.resumeFileMapper = resumeFileMapper;
        this.resumeMapper = resumeMapper;
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.uploadProperties = uploadProperties;
    }

    @Override
    @Transactional
    public ResumeFileVO upload(Long resumeId, MultipartFile file) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireOwnedResume(resumeId, user.userId());
        validateFile(file);
        ensureFileCountLimit(resume.getId());

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String fileExt = requireAllowedExtension(originalName);
        String contentType = normalizeContentType(file.getContentType());
        ensureAllowedContentType(contentType);

        String storedName = generateStoredName(fileExt);
        String storagePath = String.join(
                "/",
                "resumes",
                user.userId().toString(),
                resume.getId().toString(),
                storedName
        );
        Path targetPath = resolveStoragePath(storagePath);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "failed to save resume file", ex);
        }

        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setResumeId(resume.getId());
        resumeFile.setUserId(user.userId());
        resumeFile.setOriginalName(truncate(originalName, MAX_ORIGINAL_NAME_LENGTH));
        resumeFile.setStoredName(storedName);
        resumeFile.setStoragePath(storagePath);
        resumeFile.setContentType(contentType);
        resumeFile.setFileSize(file.getSize());
        resumeFile.setFileExt(fileExt);
        try {
            resumeFileMapper.insert(resumeFile);
        } catch (RuntimeException ex) {
            deleteFileQuietly(targetPath);
            throw ex;
        }
        return toVO(resumeFileMapper.selectById(resumeFile.getId()));
    }

    @Override
    public List<ResumeFileVO> listMine(Long resumeId) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireOwnedResume(resumeId, user.userId());
        return resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getResumeId, resume.getId())
                        .eq(ResumeFile::getUserId, user.userId())
                        .orderByDesc(ResumeFile::getCreatedAt)
                        .orderByDesc(ResumeFile::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public DownloadFile getDownload(Long fileId) {
        LoginUser user = LoginUserSupport.currentUser();
        ResumeFile resumeFile = requireDownloadableResumeFile(fileId, user);
        Path filePath = resolveStoragePath(resumeFile.getStoragePath());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new BizException(ResultCode.BIZ_ERROR, "file not found");
        }
        Resource resource = new PathResource(filePath);
        return new DownloadFile(
                resource,
                resumeFile.getOriginalName(),
                resumeFile.getContentType(),
                resumeFile.getFileSize()
        );
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        ResumeFile resumeFile = requireOwnedResumeFile(fileId, user.userId());
        resumeFileMapper.deleteById(resumeFile.getId());
        deleteFileQuietly(resolveStoragePath(resumeFile.getStoragePath()));
    }

    @Override
    public boolean existsByResumeId(Long resumeId) {
        return resumeFileMapper.selectCount(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getResumeId, resumeId)) > 0;
    }

    private Resume requireOwnedResume(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "resume not found");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "resume does not belong to current user");
        }
        return resume;
    }

    private ResumeFile requireOwnedResumeFile(Long fileId, Long userId) {
        ResumeFile resumeFile = resumeFileMapper.selectById(fileId);
        if (resumeFile == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "file not found");
        }
        if (!userId.equals(resumeFile.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "file does not belong to current user");
        }
        return resumeFile;
    }

    private ResumeFile requireDownloadableResumeFile(Long fileId, LoginUser user) {
        ResumeFile resumeFile = resumeFileMapper.selectById(fileId);
        if (resumeFile == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "file not found");
        }
        if (LoginUserSupport.ROLE_USER.equals(user.role())) {
            if (!user.userId().equals(resumeFile.getUserId())) {
                throw new BizException(ResultCode.FORBIDDEN, "file does not belong to current user");
            }
            return resumeFile;
        }
        if (LoginUserSupport.ROLE_ENTERPRISE.equals(user.role())) {
            ensureEnterpriseCanAccess(resumeFile, user.userId());
            return resumeFile;
        }
        throw new BizException(ResultCode.FORBIDDEN, "resume file download is not allowed for current role");
    }

    private void ensureEnterpriseCanAccess(ResumeFile resumeFile, Long enterpriseId) {
        List<JobApplication> applications = applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getResumeId, resumeFile.getResumeId()));
        if (applications.isEmpty()) {
            throw new BizException(ResultCode.FORBIDDEN, "resume file is not visible to current enterprise");
        }
        List<Long> jobIds = applications.stream()
                .map(JobApplication::getJobId)
                .distinct()
                .toList();
        boolean visible = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .in(Job::getId, jobIds)
                .eq(Job::getEnterpriseId, enterpriseId)) > 0;
        if (!visible) {
            throw new BizException(ResultCode.FORBIDDEN, "resume file is not visible to current enterprise");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "file must not be empty");
        }
        if (file.getSize() > uploadProperties.getMaxFileSize().toBytes()) {
            throw new BizException(ResultCode.BIZ_ERROR, "file size must not exceed 10MB");
        }
    }

    private void ensureFileCountLimit(Long resumeId) {
        long count = resumeFileMapper.selectCount(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getResumeId, resumeId));
        if (count >= MAX_FILE_COUNT_PER_RESUME) {
            throw new BizException(ResultCode.BIZ_ERROR, "a resume can have at most 3 files");
        }
    }

    private String normalizeOriginalName(String originalFilename) {
        String originalName = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "resume";
        originalName = originalName.replace('\\', '/');
        int lastSlash = originalName.lastIndexOf('/');
        if (lastSlash >= 0) {
            originalName = originalName.substring(lastSlash + 1);
        }
        originalName = StringUtils.cleanPath(originalName);
        if (!StringUtils.hasText(originalName) || originalName.equals(".") || originalName.equals("..")) {
            throw new BizException(ResultCode.BIZ_ERROR, "invalid file name");
        }
        return originalName;
    }

    private String requireAllowedExtension(String originalName) {
        String ext = StringUtils.getFilenameExtension(originalName);
        if (!StringUtils.hasText(ext)) {
            throw new BizException(ResultCode.BIZ_ERROR, "file extension is required");
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        boolean allowed = uploadProperties.getAllowedExtensions().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
        if (!allowed) {
            throw new BizException(ResultCode.BIZ_ERROR, "file extension must be pdf, doc or docx");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim().toLowerCase(Locale.ROOT) : null;
    }

    private void ensureAllowedContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.BIZ_ERROR, "file content type is not allowed");
        }
    }

    private String generateStoredName(String fileExt) {
        String timestamp = LocalDateTime.now().format(STORED_NAME_TIME_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "_" + uuid + "." + fileExt;
    }

    private Path resolveStoragePath(String storagePath) {
        Path basePath = Path.of(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(storagePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BizException(ResultCode.BIZ_ERROR, "invalid storage path");
        }
        return targetPath;
    }

    private void deleteFileQuietly(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Failed to delete resume file: {}", filePath, ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private ResumeFileVO toVO(ResumeFile resumeFile) {
        return new ResumeFileVO(
                resumeFile.getId(),
                resumeFile.getResumeId(),
                resumeFile.getOriginalName(),
                resumeFile.getContentType(),
                resumeFile.getFileSize(),
                resumeFile.getFileExt(),
                resumeFile.getCreatedAt()
        );
    }
}
