package dev.deriou.airesume.redis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RedisKeys {

    public static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);
    public static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(30);
    public static final Duration HOT_CACHE_TTL_BASE = Duration.ofMinutes(5);
    public static final Duration HOT_CACHE_EMPTY_TTL = Duration.ofSeconds(30);

    private static final String CAPTCHA_PREFIX = "airesume:login:captcha:";
    private static final String LOGIN_TOKEN_PREFIX = "airesume:login:token:";
    private static final String CHECK_IN_PREFIX = "airesume:sign:";
    private static final String HOT_JOBS_KEY = "airesume:cache:hot:jobs";
    private static final String HOT_COMPANIES_KEY = "airesume:cache:hot:companies";

    private RedisKeys() {
    }

    public static String captcha(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }

    public static String loginToken(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }

    public static String checkIn(Long userId, LocalDate date) {
        return CHECK_IN_PREFIX + userId + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    public static String hotJobs() {
        return HOT_JOBS_KEY;
    }

    public static String hotCompanies() {
        return HOT_COMPANIES_KEY;
    }
}
