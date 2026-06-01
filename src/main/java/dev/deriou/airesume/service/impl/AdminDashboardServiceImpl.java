package dev.deriou.airesume.service.impl;

import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.service.AdminDashboardService;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.vo.AdminApplicationVO;
import dev.deriou.airesume.vo.AdminCreditDailyVO;
import dev.deriou.airesume.vo.AdminCreditSummaryVO;
import dev.deriou.airesume.vo.AdminCreditTopUserVO;
import dev.deriou.airesume.vo.AdminJobVO;
import dev.deriou.airesume.vo.AdminLlmDailyVO;
import dev.deriou.airesume.vo.AdminLlmOperationVO;
import dev.deriou.airesume.vo.AdminLlmSummaryVO;
import dev.deriou.airesume.vo.AdminOverviewVO;
import dev.deriou.airesume.vo.PageVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int STAT_DAYS = 7;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminOverviewVO overview() {
        requireAdmin();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return new AdminOverviewVO(
                queryLong("SELECT COUNT(*) FROM app_user"),
                queryLong("SELECT COUNT(*) FROM app_user WHERE role = 'USER'"),
                queryLong("SELECT COUNT(*) FROM app_user WHERE role = 'ENTERPRISE'"),
                queryLong("SELECT COUNT(*) FROM app_user WHERE role = 'ADMIN'"),
                queryLong("SELECT COUNT(*) FROM resume"),
                queryLong("SELECT COUNT(*) FROM job"),
                queryLong("SELECT COUNT(*) FROM job WHERE status = 'OPEN'"),
                queryLong("SELECT COUNT(*) FROM application"),
                queryLong("SELECT COUNT(*) FROM llm_call_log WHERE created_at >= ? AND created_at < ?", start, end),
                queryLong("SELECT COALESCE(SUM(total_tokens), 0) FROM llm_call_log WHERE created_at >= ? AND created_at < ?", start, end),
                queryLong("SELECT COALESCE(SUM(credit_cost), 0) FROM llm_call_log WHERE created_at >= ? AND created_at < ?", start, end),
                queryLong("SELECT COALESCE(ROUND(AVG(latency_ms)), 0) FROM llm_call_log WHERE created_at >= ? AND created_at < ?", start, end),
                queryLong("SELECT COUNT(*) FROM llm_call_log WHERE status = ? AND created_at >= ? AND created_at < ?", STATUS_FAILED, start, end)
        );
    }

    @Override
    public AdminLlmSummaryVO llmSummary() {
        requireAdmin();
        Period period = statPeriod();
        return jdbcTemplate.queryForObject("""
                SELECT
                  COUNT(*) AS call_count,
                  COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count,
                  COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failure_count,
                  COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                  COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
                  COALESCE(SUM(total_tokens), 0) AS total_tokens,
                  COALESCE(ROUND(AVG(latency_ms)), 0) AS avg_latency_ms,
                  COALESCE(SUM(credit_cost), 0) AS credit_cost
                FROM llm_call_log
                WHERE created_at >= ? AND created_at < ?
                """, (rs, rowNum) -> {
            long callCount = rs.getLong("call_count");
            long failureCount = rs.getLong("failure_count");
            return new AdminLlmSummaryVO(
                    STAT_DAYS,
                    callCount,
                    rs.getLong("success_count"),
                    failureCount,
                    callCount == 0 ? 0.0 : (double) failureCount / callCount,
                    rs.getLong("prompt_tokens"),
                    rs.getLong("completion_tokens"),
                    rs.getLong("total_tokens"),
                    rs.getLong("avg_latency_ms"),
                    rs.getLong("credit_cost")
            );
        }, period.start(), period.end());
    }

    @Override
    public List<AdminLlmDailyVO> llmDaily() {
        requireAdmin();
        Period period = statPeriod();
        Map<LocalDate, AdminLlmDailyVO> values = new HashMap<>();
        jdbcTemplate.query("""
                SELECT
                  DATE(created_at) AS stat_date,
                  COUNT(*) AS call_count,
                  COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count,
                  COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failure_count,
                  COALESCE(SUM(total_tokens), 0) AS total_tokens,
                  COALESCE(SUM(credit_cost), 0) AS credit_cost,
                  COALESCE(ROUND(AVG(latency_ms)), 0) AS avg_latency_ms
                FROM llm_call_log
                WHERE created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                ORDER BY stat_date ASC
                """, rs -> {
            LocalDate date = rs.getDate("stat_date").toLocalDate();
            values.put(date, new AdminLlmDailyVO(
                    date,
                    rs.getLong("call_count"),
                    rs.getLong("success_count"),
                    rs.getLong("failure_count"),
                    rs.getLong("total_tokens"),
                    rs.getLong("credit_cost"),
                    rs.getLong("avg_latency_ms")
            ));
        }, period.start(), period.end());
        List<AdminLlmDailyVO> result = new ArrayList<>();
        for (int i = 0; i < STAT_DAYS; i++) {
            LocalDate date = period.firstDate().plusDays(i);
            result.add(values.getOrDefault(date, new AdminLlmDailyVO(date, 0, 0, 0, 0, 0, 0)));
        }
        return result;
    }

    @Override
    public List<AdminLlmOperationVO> llmOperations() {
        requireAdmin();
        Period period = statPeriod();
        return jdbcTemplate.query("""
                SELECT
                  operation,
                  COUNT(*) AS call_count,
                  COALESCE(SUM(total_tokens), 0) AS total_tokens,
                  COALESCE(SUM(credit_cost), 0) AS credit_cost,
                  COALESCE(ROUND(AVG(latency_ms)), 0) AS avg_latency_ms
                FROM llm_call_log
                WHERE created_at >= ? AND created_at < ?
                GROUP BY operation
                ORDER BY call_count DESC, operation ASC
                """, (rs, rowNum) -> new AdminLlmOperationVO(
                rs.getString("operation"),
                rs.getLong("call_count"),
                rs.getLong("total_tokens"),
                rs.getLong("credit_cost"),
                rs.getLong("avg_latency_ms")
        ), period.start(), period.end());
    }

    @Override
    public AdminCreditSummaryVO creditSummary() {
        requireAdmin();
        Period period = statPeriod();
        return jdbcTemplate.queryForObject("""
                SELECT
                  COALESCE(SUM(CASE WHEN change_amount > 0 THEN change_amount ELSE 0 END), 0) AS granted_credits,
                  COALESCE(SUM(CASE WHEN change_amount < 0 THEN -change_amount ELSE 0 END), 0) AS consumed_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN change_amount ELSE 0 END), 0) AS check_in_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN change_amount ELSE 0 END), 0) AS admin_grant_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN -change_amount ELSE 0 END), 0) AS resume_score_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN -change_amount ELSE 0 END), 0) AS resume_optimize_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN -change_amount ELSE 0 END), 0) AS job_match_credits
                FROM credit_transaction
                WHERE created_at >= ? AND created_at < ?
                """, (rs, rowNum) -> new AdminCreditSummaryVO(
                STAT_DAYS,
                rs.getLong("granted_credits"),
                rs.getLong("consumed_credits"),
                rs.getLong("check_in_credits"),
                rs.getLong("admin_grant_credits"),
                rs.getLong("resume_score_credits"),
                rs.getLong("resume_optimize_credits"),
                rs.getLong("job_match_credits")
        ), CreditService.TYPE_CHECK_IN,
                CreditService.TYPE_ADMIN_GRANT,
                CreditService.TYPE_AI_RESUME_SCORE,
                CreditService.TYPE_AI_RESUME_OPTIMIZE,
                CreditService.TYPE_AI_JOB_MATCH,
                period.start(),
                period.end());
    }

    @Override
    public List<AdminCreditDailyVO> creditDaily() {
        requireAdmin();
        Period period = statPeriod();
        Map<LocalDate, AdminCreditDailyVO> values = new HashMap<>();
        jdbcTemplate.query("""
                SELECT
                  DATE(created_at) AS stat_date,
                  COALESCE(SUM(CASE WHEN change_amount > 0 THEN change_amount ELSE 0 END), 0) AS granted_credits,
                  COALESCE(SUM(CASE WHEN change_amount < 0 THEN -change_amount ELSE 0 END), 0) AS consumed_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN change_amount ELSE 0 END), 0) AS check_in_credits,
                  COALESCE(SUM(CASE WHEN type = ? THEN change_amount ELSE 0 END), 0) AS admin_grant_credits,
                  COALESCE(SUM(CASE WHEN type IN (?, ?, ?) THEN -change_amount ELSE 0 END), 0) AS ai_consumed_credits
                FROM credit_transaction
                WHERE created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                ORDER BY stat_date ASC
                """, rs -> {
            LocalDate date = rs.getDate("stat_date").toLocalDate();
            values.put(date, new AdminCreditDailyVO(
                    date,
                    rs.getLong("granted_credits"),
                    rs.getLong("consumed_credits"),
                    rs.getLong("check_in_credits"),
                    rs.getLong("admin_grant_credits"),
                    rs.getLong("ai_consumed_credits")
            ));
        }, CreditService.TYPE_CHECK_IN,
                CreditService.TYPE_ADMIN_GRANT,
                CreditService.TYPE_AI_RESUME_SCORE,
                CreditService.TYPE_AI_RESUME_OPTIMIZE,
                CreditService.TYPE_AI_JOB_MATCH,
                period.start(),
                period.end());
        List<AdminCreditDailyVO> result = new ArrayList<>();
        for (int i = 0; i < STAT_DAYS; i++) {
            LocalDate date = period.firstDate().plusDays(i);
            result.add(values.getOrDefault(date, new AdminCreditDailyVO(date, 0, 0, 0, 0, 0)));
        }
        return result;
    }

    @Override
    public List<AdminCreditTopUserVO> creditTopUsers(int limit) {
        requireAdmin();
        Period period = statPeriod();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return jdbcTemplate.query("""
                SELECT
                  u.id AS user_id,
                  u.username,
                  u.nick_name,
                  c.consumed_credits,
                  COALESCE(l.total_tokens, 0) AS total_tokens,
                  COALESCE(l.ai_call_count, 0) AS ai_call_count
                FROM app_user u
                JOIN (
                  SELECT user_id, SUM(-change_amount) AS consumed_credits
                  FROM credit_transaction
                  WHERE created_at >= ? AND created_at < ?
                    AND type IN (?, ?, ?)
                    AND change_amount < 0
                  GROUP BY user_id
                ) c ON c.user_id = u.id
                LEFT JOIN (
                  SELECT user_id, SUM(total_tokens) AS total_tokens, COUNT(*) AS ai_call_count
                  FROM llm_call_log
                  WHERE created_at >= ? AND created_at < ?
                    AND status = ?
                  GROUP BY user_id
                ) l ON l.user_id = u.id
                ORDER BY c.consumed_credits DESC, total_tokens DESC, u.id ASC
                LIMIT ?
                """, (rs, rowNum) -> new AdminCreditTopUserVO(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("nick_name"),
                rs.getLong("consumed_credits"),
                rs.getLong("total_tokens"),
                rs.getLong("ai_call_count")
        ), period.start(),
                period.end(),
                CreditService.TYPE_AI_RESUME_SCORE,
                CreditService.TYPE_AI_RESUME_OPTIMIZE,
                CreditService.TYPE_AI_JOB_MATCH,
                period.start(),
                period.end(),
                STATUS_SUCCESS,
                safeLimit);
    }

    @Override
    public PageVO<AdminJobVO> jobs(long page, long size, String status, String keyword) {
        requireAdmin();
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        long offset = (current - 1) * pageSize;
        List<Object> params = new ArrayList<>();
        String where = buildJobWhere(status, keyword, params);
        long total = queryLong("SELECT COUNT(*) FROM job j JOIN app_user e ON e.id = j.enterprise_id " + where, params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<AdminJobVO> records = jdbcTemplate.query("""
                SELECT
                  j.id,
                  j.enterprise_id,
                  e.nick_name AS enterprise_name,
                  j.title,
                  j.tech_stack,
                  j.location,
                  j.status,
                  COUNT(a.id) AS application_count,
                  j.created_at,
                  j.updated_at
                FROM job j
                JOIN app_user e ON e.id = j.enterprise_id
                LEFT JOIN application a ON a.job_id = j.id
                """ + where + """
                GROUP BY j.id, j.enterprise_id, e.nick_name, j.title, j.tech_stack, j.location, j.status, j.created_at, j.updated_at
                ORDER BY j.updated_at DESC, j.id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new AdminJobVO(
                rs.getLong("id"),
                rs.getLong("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getString("title"),
                rs.getString("tech_stack"),
                rs.getString("location"),
                rs.getString("status"),
                rs.getLong("application_count"),
                localDateTime(rs, "created_at"),
                localDateTime(rs, "updated_at")
        ), listParams.toArray());
        return PageVO.of(records, current, pageSize, total, pages(total, pageSize));
    }

    @Override
    public PageVO<AdminApplicationVO> applications(long page, long size, String status) {
        requireAdmin();
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        long offset = (current - 1) * pageSize;
        List<Object> params = new ArrayList<>();
        String where = buildApplicationWhere(status, params);
        long total = queryLong("""
                SELECT COUNT(*)
                FROM application a
                JOIN app_user u ON u.id = a.user_id
                JOIN resume r ON r.id = a.resume_id
                JOIN job j ON j.id = a.job_id
                JOIN app_user e ON e.id = j.enterprise_id
                """ + where, params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<AdminApplicationVO> records = jdbcTemplate.query("""
                SELECT
                  a.id,
                  a.user_id,
                  u.username,
                  u.nick_name,
                  a.resume_id,
                  r.title AS resume_title,
                  a.job_id,
                  j.title AS job_title,
                  j.enterprise_id,
                  e.nick_name AS enterprise_name,
                  a.status,
                  a.remark,
                  a.reviewed_by,
                  a.reviewed_at,
                  a.created_at,
                  a.updated_at
                FROM application a
                JOIN app_user u ON u.id = a.user_id
                JOIN resume r ON r.id = a.resume_id
                JOIN job j ON j.id = a.job_id
                JOIN app_user e ON e.id = j.enterprise_id
                """ + where + """
                ORDER BY a.updated_at DESC, a.id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new AdminApplicationVO(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("nick_name"),
                rs.getLong("resume_id"),
                rs.getString("resume_title"),
                rs.getLong("job_id"),
                rs.getString("job_title"),
                rs.getLong("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getString("status"),
                rs.getString("remark"),
                nullableLong(rs, "reviewed_by"),
                localDateTime(rs, "reviewed_at"),
                localDateTime(rs, "created_at"),
                localDateTime(rs, "updated_at")
        ), listParams.toArray());
        return PageVO.of(records, current, pageSize, total, pages(total, pageSize));
    }

    private String buildJobWhere(String status, String keyword, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (StringUtils.hasText(status)) {
            String normalized = status.trim().toUpperCase();
            if (!JobServiceImpl.STATUS_OPEN.equals(normalized) && !JobServiceImpl.STATUS_CLOSED.equals(normalized)) {
                throw new BizException(ResultCode.BIZ_ERROR, "status must be OPEN or CLOSED");
            }
            where.append(" AND j.status = ?");
            params.add(normalized);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (j.title LIKE ? OR e.nick_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        return where.toString();
    }

    private String buildApplicationWhere(String status, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (StringUtils.hasText(status)) {
            String normalized = status.trim().toUpperCase();
            if (!"PENDING".equals(normalized)
                    && !"VIEWED".equals(normalized)
                    && !"REJECTED".equals(normalized)
                    && !"ACCEPTED".equals(normalized)) {
                throw new BizException(ResultCode.BIZ_ERROR, "status must be PENDING, VIEWED, REJECTED or ACCEPTED");
            }
            where.append(" AND a.status = ?");
            params.add(normalized);
        }
        return where.toString();
    }

    private void requireAdmin() {
        LoginUserSupport.requireRole(LoginUserSupport.ROLE_ADMIN);
    }

    private Period statPeriod() {
        LocalDate firstDate = LocalDate.now().minusDays(STAT_DAYS - 1L);
        return new Period(firstDate, firstDate.atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
    }

    private long queryLong(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value != null ? value.longValue() : 0L;
    }

    private long pages(long total, long size) {
        return total == 0 ? 0 : (total + size - 1) / size;
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record Period(LocalDate firstDate, LocalDateTime start, LocalDateTime end) {
    }
}
