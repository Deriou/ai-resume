package dev.deriou.airesume.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.deriou.airesume.redis.RedisKeys;
import dev.deriou.airesume.service.HotDataService;
import dev.deriou.airesume.vo.HotCompanyVO;
import dev.deriou.airesume.vo.HotJobVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HotDataServiceImpl implements HotDataService {

    private static final Logger log = LoggerFactory.getLogger(HotDataServiceImpl.class);
    private static final int HOT_LIMIT = 10;
    private static final long RANDOM_TTL_SECONDS = 120;

    private static final String HOT_JOBS_SQL = """
            SELECT
              j.id AS job_id,
              j.title,
              j.enterprise_id,
              j.location,
              j.tech_stack,
              COUNT(a.id) AS application_count
            FROM job j
            LEFT JOIN application a ON a.job_id = j.id
            WHERE j.status = 'OPEN'
            GROUP BY j.id, j.title, j.enterprise_id, j.location, j.tech_stack, j.created_at
            ORDER BY application_count DESC, j.created_at DESC, j.id DESC
            LIMIT ?
            """;

    private static final String HOT_COMPANIES_SQL = """
            SELECT
              u.id AS enterprise_id,
              u.nick_name AS enterprise_name,
              COUNT(DISTINCT CASE WHEN j.status = 'OPEN' THEN j.id END) AS open_job_count,
              COUNT(a.id) AS application_count
            FROM app_user u
            LEFT JOIN job j ON j.enterprise_id = u.id
            LEFT JOIN application a ON a.job_id = j.id
            WHERE u.role = 'ENTERPRISE'
            GROUP BY u.id, u.nick_name
            ORDER BY application_count DESC, open_job_count DESC, u.id ASC
            LIMIT ?
            """;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public HotDataServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<HotJobVO> listHotJobs() {
        List<HotJobVO> cached = readCache(RedisKeys.hotJobs(), new TypeReference<List<HotJobVO>>() {
        });
        if (cached != null) {
            return cached;
        }
        List<HotJobVO> jobs = queryHotJobs();
        writeCache(RedisKeys.hotJobs(), jobs);
        return jobs;
    }

    @Override
    public List<HotCompanyVO> listHotCompanies() {
        List<HotCompanyVO> cached = readCache(RedisKeys.hotCompanies(), new TypeReference<List<HotCompanyVO>>() {
        });
        if (cached != null) {
            return cached;
        }
        List<HotCompanyVO> companies = queryHotCompanies();
        writeCache(RedisKeys.hotCompanies(), companies);
        return companies;
    }

    @Override
    public void refreshAll() {
        writeCache(RedisKeys.hotJobs(), queryHotJobs());
        writeCache(RedisKeys.hotCompanies(), queryHotCompanies());
    }

    @Override
    public void evictHotCaches() {
        try {
            stringRedisTemplate.delete(List.of(RedisKeys.hotJobs(), RedisKeys.hotCompanies()));
        } catch (RuntimeException ex) {
            log.warn("failed to evict hot caches", ex);
        }
    }

    private List<HotJobVO> queryHotJobs() {
        return jdbcTemplate.query(HOT_JOBS_SQL, this::mapHotJob, HOT_LIMIT);
    }

    private List<HotCompanyVO> queryHotCompanies() {
        return jdbcTemplate.query(HOT_COMPANIES_SQL, this::mapHotCompany, HOT_LIMIT);
    }

    private HotJobVO mapHotJob(ResultSet rs, int rowNum) throws SQLException {
        return new HotJobVO(
                rs.getLong("job_id"),
                rs.getString("title"),
                rs.getLong("enterprise_id"),
                rs.getString("location"),
                rs.getString("tech_stack"),
                rs.getLong("application_count")
        );
    }

    private HotCompanyVO mapHotCompany(ResultSet rs, int rowNum) throws SQLException {
        return new HotCompanyVO(
                rs.getLong("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getLong("open_job_count"),
                rs.getLong("application_count")
        );
    }

    private <T> List<T> readCache(String key, TypeReference<List<T>> typeReference) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            log.warn("failed to parse hot cache: key={}", key, ex);
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private void writeCache(String key, List<?> data) {
        try {
            Duration ttl = data.isEmpty() ? RedisKeys.HOT_CACHE_EMPTY_TTL : randomHotTtl();
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttl);
        } catch (JsonProcessingException ex) {
            log.warn("failed to serialize hot cache: key={}", key, ex);
        }
    }

    private Duration randomHotTtl() {
        long seconds = RedisKeys.HOT_CACHE_TTL_BASE.toSeconds()
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1);
        return Duration.ofSeconds(seconds);
    }
}
