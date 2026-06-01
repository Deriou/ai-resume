package dev.deriou.airesume.redis;

import java.time.Duration;

public final class RedisKeys {

    public static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);
    public static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(30);

    private static final String CAPTCHA_PREFIX = "airesume:login:captcha:";
    private static final String LOGIN_TOKEN_PREFIX = "airesume:login:token:";

    private RedisKeys() {
    }

    public static String captcha(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }

    public static String loginToken(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }
}
