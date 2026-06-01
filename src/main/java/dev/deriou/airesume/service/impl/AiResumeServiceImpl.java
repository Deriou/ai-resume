package dev.deriou.airesume.service.impl;

import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.JobMatchRequest;
import dev.deriou.airesume.dto.ResumeOptimizeRequest;
import dev.deriou.airesume.dto.ResumeScoreRequest;
import dev.deriou.airesume.entity.Job;
import dev.deriou.airesume.entity.JobMatch;
import dev.deriou.airesume.entity.Resume;
import dev.deriou.airesume.entity.ResumeScore;
import dev.deriou.airesume.llm.DeepSeekClient;
import dev.deriou.airesume.llm.LlmJsonParser;
import dev.deriou.airesume.llm.LlmProperties;
import dev.deriou.airesume.llm.LlmResult;
import dev.deriou.airesume.llm.PromptBuilder;
import dev.deriou.airesume.mapper.JobMapper;
import dev.deriou.airesume.mapper.JobMatchMapper;
import dev.deriou.airesume.mapper.ResumeMapper;
import dev.deriou.airesume.mapper.ResumeScoreMapper;
import dev.deriou.airesume.service.AiResumeService;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.service.LlmCallLogService;
import dev.deriou.airesume.vo.JobMatchVO;
import dev.deriou.airesume.vo.ResumeOptimizeVO;
import dev.deriou.airesume.vo.ResumeScoreVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiResumeServiceImpl implements AiResumeService {

    private static final Logger log = LoggerFactory.getLogger(AiResumeServiceImpl.class);

    private static final String OPERATION_RESUME_SCORE = "RESUME_SCORE";
    private static final String OPERATION_RESUME_OPTIMIZE = "RESUME_OPTIMIZE";
    private static final String OPERATION_JOB_MATCH = "JOB_MATCH";
    private static final int CREDIT_COST_RESUME_SCORE = 1;
    private static final int CREDIT_COST_RESUME_OPTIMIZE = 2;
    private static final int CREDIT_COST_JOB_MATCH = 1;

    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;
    private final ResumeScoreMapper resumeScoreMapper;
    private final JobMatchMapper jobMatchMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptBuilder promptBuilder;
    private final LlmJsonParser llmJsonParser;
    private final LlmCallLogService llmCallLogService;
    private final LlmProperties llmProperties;
    private final CreditService creditService;
    private final TransactionTemplate transactionTemplate;

    public AiResumeServiceImpl(
            ResumeMapper resumeMapper,
            JobMapper jobMapper,
            ResumeScoreMapper resumeScoreMapper,
            JobMatchMapper jobMatchMapper,
            DeepSeekClient deepSeekClient,
            PromptBuilder promptBuilder,
            LlmJsonParser llmJsonParser,
            LlmCallLogService llmCallLogService,
            LlmProperties llmProperties,
            CreditService creditService,
            PlatformTransactionManager transactionManager
    ) {
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
        this.resumeScoreMapper = resumeScoreMapper;
        this.jobMatchMapper = jobMatchMapper;
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogService = llmCallLogService;
        this.llmProperties = llmProperties;
        this.creditService = creditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public ResumeScoreVO scoreResume(Long resumeId, ResumeScoreRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireOwnResume(resumeId, user.userId());
        String targetDirection = request.targetDirection().trim();
        String prompt = promptBuilder.buildResumeScorePrompt(resume, targetDirection);
        creditService.ensureSufficient(user.userId(), CREDIT_COST_RESUME_SCORE);

        LlmResult result = null;
        try {
            result = deepSeekClient.chatWithUsage(prompt);
            LlmJsonParser.ParsedResumeScore parsed = llmJsonParser.parseResumeScore(result.content());
            LlmResult successResult = result;

            return transactionTemplate.execute(status -> {
                ResumeScore score = new ResumeScore();
                score.setResumeId(resume.getId());
                score.setTargetDirection(targetDirection);
                score.setOverallScore(parsed.overallScore());
                score.setDimensionsJson(llmJsonParser.toJson(parsed.dimensions()));
                score.setSuggestions(llmJsonParser.toJson(parsed.suggestions()));
                score.setLlmModel(successResult.model());
                score.setPromptTokens(successResult.promptTokens());
                score.setCompletionTokens(successResult.completionTokens());
                score.setTotalTokens(successResult.totalTokens());
                resumeScoreMapper.insert(score);

                Long logId = llmCallLogService.recordSuccess(user.userId(), OPERATION_RESUME_SCORE, successResult);
                creditService.charge(
                        user.userId(),
                        CREDIT_COST_RESUME_SCORE,
                        CreditService.TYPE_AI_RESUME_SCORE,
                        CreditService.REF_TYPE_LLM_CALL_LOG,
                        logId,
                        "resume score"
                );
                llmCallLogService.updateCreditCost(logId, CREDIT_COST_RESUME_SCORE);

                ResumeScore saved = resumeScoreMapper.selectById(score.getId());
                return new ResumeScoreVO(
                        saved.getId(),
                        saved.getResumeId(),
                        saved.getTargetDirection(),
                        saved.getOverallScore(),
                        parsed.dimensions(),
                        parsed.suggestions(),
                        saved.getLlmModel(),
                        saved.getPromptTokens(),
                        saved.getCompletionTokens(),
                        saved.getTotalTokens(),
                        saved.getCreatedAt()
                );
            });
        } catch (RuntimeException ex) {
            safeRecordFailure(user.userId(), OPERATION_RESUME_SCORE, result, ex);
            throw ex;
        }
    }

    @Override
    public ResumeOptimizeVO optimizeResume(Long resumeId, ResumeOptimizeRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireOwnResume(resumeId, user.userId());
        String targetDirection = request.targetDirection().trim();
        String prompt = promptBuilder.buildResumeOptimizePrompt(resume, targetDirection);
        creditService.ensureSufficient(user.userId(), CREDIT_COST_RESUME_OPTIMIZE);

        LlmResult result = null;
        try {
            result = deepSeekClient.chatWithUsage(prompt);
            LlmJsonParser.ParsedResumeOptimize parsed = llmJsonParser.parseResumeOptimize(result.content());
            LlmResult successResult = result;

            return transactionTemplate.execute(status -> {
                Long logId = llmCallLogService.recordSuccess(user.userId(), OPERATION_RESUME_OPTIMIZE, successResult);
                creditService.charge(
                        user.userId(),
                        CREDIT_COST_RESUME_OPTIMIZE,
                        CreditService.TYPE_AI_RESUME_OPTIMIZE,
                        CreditService.REF_TYPE_LLM_CALL_LOG,
                        logId,
                        "resume optimize"
                );
                llmCallLogService.updateCreditCost(logId, CREDIT_COST_RESUME_OPTIMIZE);
                return new ResumeOptimizeVO(
                        parsed.summary(),
                        parsed.optimizedBullets(),
                        parsed.rewriteSuggestions(),
                        successResult.model(),
                        successResult.promptTokens(),
                        successResult.completionTokens(),
                        successResult.totalTokens(),
                        successResult.latencyMs()
                );
            });
        } catch (RuntimeException ex) {
            safeRecordFailure(user.userId(), OPERATION_RESUME_OPTIMIZE, result, ex);
            throw ex;
        }
    }

    @Override
    public JobMatchVO matchJob(Long jobId, JobMatchRequest request) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        Resume resume = requireOwnResume(request.resumeId(), user.userId());
        Job job = requireOpenJob(jobId);
        String prompt = promptBuilder.buildJobMatchPrompt(resume, job);
        creditService.ensureSufficient(user.userId(), CREDIT_COST_JOB_MATCH);

        LlmResult result = null;
        try {
            result = deepSeekClient.chatWithUsage(prompt);
            LlmJsonParser.ParsedJobMatch parsed = llmJsonParser.parseJobMatch(result.content());
            LlmResult successResult = result;

            return transactionTemplate.execute(status -> {
                JobMatch match = new JobMatch();
                match.setResumeId(resume.getId());
                match.setJobId(job.getId());
                match.setMatchScore(parsed.matchScore());
                match.setStrengths(llmJsonParser.toJson(parsed.strengths()));
                match.setGaps(llmJsonParser.toJson(parsed.gaps()));
                match.setSuggestions(llmJsonParser.toJson(parsed.suggestions()));
                jobMatchMapper.insert(match);

                Long logId = llmCallLogService.recordSuccess(user.userId(), OPERATION_JOB_MATCH, successResult);
                creditService.charge(
                        user.userId(),
                        CREDIT_COST_JOB_MATCH,
                        CreditService.TYPE_AI_JOB_MATCH,
                        CreditService.REF_TYPE_LLM_CALL_LOG,
                        logId,
                        "job match"
                );
                llmCallLogService.updateCreditCost(logId, CREDIT_COST_JOB_MATCH);

                JobMatch saved = jobMatchMapper.selectById(match.getId());
                return new JobMatchVO(
                        saved.getId(),
                        saved.getResumeId(),
                        saved.getJobId(),
                        saved.getMatchScore(),
                        parsed.strengths(),
                        parsed.gaps(),
                        parsed.suggestions(),
                        saved.getCreatedAt()
                );
            });
        } catch (RuntimeException ex) {
            safeRecordFailure(user.userId(), OPERATION_JOB_MATCH, result, ex);
            throw ex;
        }
    }

    private void safeRecordFailure(Long userId, String operation, LlmResult result, RuntimeException ex) {
        try {
            llmCallLogService.recordFailure(userId, operation, llmProperties.model(), result, ex);
        } catch (RuntimeException logEx) {
            log.warn("failed to record llm failure log: operation={}, userId={}", operation, userId, logEx);
        }
    }

    private Resume requireOwnResume(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "resume not found");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "resume does not belong to current user");
        }
        return resume;
    }

    private Job requireOpenJob(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "job not found");
        }
        if (!JobServiceImpl.STATUS_OPEN.equals(job.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "job is closed");
        }
        return job;
    }
}
