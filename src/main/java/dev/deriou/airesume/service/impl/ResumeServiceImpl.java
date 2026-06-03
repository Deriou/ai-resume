package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.config.UploadProperties;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.ResumeCreateRequest;
import dev.deriou.airesume.dto.ResumeUpdateRequest;
import dev.deriou.airesume.entity.Resume;
import dev.deriou.airesume.entity.ResumeOptimize;
import dev.deriou.airesume.entity.ResumeScore;
import dev.deriou.airesume.mapper.ResumeMapper;
import dev.deriou.airesume.mapper.ResumeOptimizeMapper;
import dev.deriou.airesume.mapper.ResumeScoreMapper;
import dev.deriou.airesume.service.ResumeFileService;
import dev.deriou.airesume.service.ResumeService;
import dev.deriou.airesume.vo.PageVO;
import dev.deriou.airesume.vo.ResumeOptimizeRecordVO;
import dev.deriou.airesume.vo.ResumeScoreSummaryVO;
import dev.deriou.airesume.vo.ResumeVO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ResumeMapper resumeMapper;
    private final ResumeScoreMapper resumeScoreMapper;
    private final ResumeOptimizeMapper resumeOptimizeMapper;
    private final ResumeFileService resumeFileService;
    private final UploadProperties uploadProperties;
    private final ObjectMapper objectMapper;

    public ResumeServiceImpl(
            ResumeMapper resumeMapper,
            ResumeScoreMapper resumeScoreMapper,
            ResumeOptimizeMapper resumeOptimizeMapper,
            ResumeFileService resumeFileService,
            UploadProperties uploadProperties,
            ObjectMapper objectMapper
    ) {
        this.resumeMapper = resumeMapper;
        this.resumeScoreMapper = resumeScoreMapper;
        this.resumeOptimizeMapper = resumeOptimizeMapper;
        this.resumeFileService = resumeFileService;
        this.uploadProperties = uploadProperties;
        this.objectMapper = objectMapper;
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
    @Transactional
    public ResumeVO importPdf(MultipartFile file, String title) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        validatePdf(file);
        String extractedText = extractPdfText(file);
        if (extractedText.length() < 30) {
            throw new BizException(ResultCode.BIZ_ERROR, "pdf text is too short, please use a text-based PDF");
        }

        Resume resume = new Resume();
        resume.setUserId(user.userId());
        resume.setTitle(resolveTitle(file, title));
        resume.setContentMd(toMarkdown(extractedText));
        resumeMapper.insert(resume);
        resumeFileService.upload(resume.getId(), file);
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
        if (resumeFileService.existsByResumeId(id)) {
            throw new BizException(ResultCode.BIZ_ERROR, "resume has files, please delete files first");
        }
        resumeMapper.deleteById(id);
    }

    @Override
    public List<ResumeScoreSummaryVO> listLatestScores() {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        List<Long> resumeIds = resumeMapper.selectList(
                        new LambdaQueryWrapper<Resume>()
                                .select(Resume::getId)
                                .eq(Resume::getUserId, user.userId()))
                .stream()
                .map(Resume::getId)
                .toList();
        if (resumeIds.isEmpty()) {
            return List.of();
        }

        List<ResumeScore> scores = resumeScoreMapper.selectList(
                new LambdaQueryWrapper<ResumeScore>()
                        .in(ResumeScore::getResumeId, resumeIds)
                        .orderByDesc(ResumeScore::getCreatedAt)
                        .orderByDesc(ResumeScore::getId));

        Set<Long> seen = new LinkedHashSet<>();
        List<ResumeScoreSummaryVO> summaries = new ArrayList<>();
        for (ResumeScore score : scores) {
            if (seen.add(score.getResumeId())) {
                summaries.add(new ResumeScoreSummaryVO(
                        score.getResumeId(),
                        score.getTargetDirection(),
                        score.getOverallScore(),
                        parseStringList(score.getSuggestions()),
                        score.getCreatedAt()));
            }
        }
        return summaries;
    }

    @Override
    public List<ResumeOptimizeRecordVO> listOptimizations(Long resumeId) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireResume(resumeId);
        ensureOwner(resume, user.userId());

        List<ResumeOptimize> records = resumeOptimizeMapper.selectList(
                new LambdaQueryWrapper<ResumeOptimize>()
                        .eq(ResumeOptimize::getResumeId, resumeId)
                        .orderByDesc(ResumeOptimize::getCreatedAt)
                        .orderByDesc(ResumeOptimize::getId));

        return records.stream()
                .map(record -> new ResumeOptimizeRecordVO(
                        record.getId(),
                        record.getResumeId(),
                        record.getTargetDirection(),
                        record.getSummary(),
                        parseStringList(record.getOptimizedBullets()),
                        parseStringList(record.getRewriteSuggestions()),
                        record.getLlmModel(),
                        record.getTotalTokens(),
                        record.getLatencyMs(),
                        record.getCreatedAt()))
                .toList();
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(json, STRING_LIST);
            return parsed == null ? List.of() : parsed;
        } catch (Exception ex) {
            log.warn("failed to parse resume optimize json", ex);
            return List.of();
        }
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

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "file must not be empty");
        }
        if (file.getSize() > uploadProperties.getMaxFileSize().toBytes()) {
            throw new BizException(ResultCode.BIZ_ERROR, "file size must not exceed 10MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        if (!"pdf".equalsIgnoreCase(ext)) {
            throw new BizException(ResultCode.BIZ_ERROR, "only pdf import is supported");
        }
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) {
                throw new BizException(ResultCode.BIZ_ERROR, "encrypted pdf is not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return normalizeExtractedText(stripper.getText(document));
        } catch (IOException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "failed to parse pdf", ex);
        }
    }

    private String normalizeExtractedText(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private String resolveTitle(MultipartFile file, String title) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        String originalName = file.getOriginalFilename();
        String filename = StringUtils.hasText(originalName) ? originalName.trim() : "PDF 导入简历";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            filename = filename.substring(0, dot);
        }
        filename = filename.replace('_', ' ').replace('-', ' ').trim();
        if (!StringUtils.hasText(filename)) {
            return "PDF 导入简历";
        }
        return filename.length() > 128 ? filename.substring(0, 128) : filename;
    }

    private String toMarkdown(String text) {
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder builder = new StringBuilder();
        builder.append("# PDF 导入简历\n\n");
        builder.append("> 以下内容由 PDF 文本提取生成。扫描版 PDF 可能无法正确识别，请保存前检查并编辑。\n\n");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                builder.append(trimmed).append("\n\n");
            }
        }
        return builder.toString().trim();
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
