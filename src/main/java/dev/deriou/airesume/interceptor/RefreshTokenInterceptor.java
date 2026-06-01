package dev.deriou.airesume.interceptor;

import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.UserHolder;
import dev.deriou.airesume.redis.RedisKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request.getHeader("Authorization"));
        if (!StringUtils.hasText(token)) {
            return true;
        }

        String key = RedisKeys.loginToken(token);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return true;
        }

        LoginUser loginUser = toLoginUser(token, entries);
        if (loginUser == null) {
            stringRedisTemplate.delete(key);
            return true;
        }

        UserHolder.save(loginUser);
        stringRedisTemplate.expire(key, RedisKeys.LOGIN_TOKEN_TTL);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        UserHolder.remove();
    }

    private LoginUser toLoginUser(String token, Map<Object, Object> entries) {
        try {
            String userId = String.valueOf(entries.get("userId"));
            String username = String.valueOf(entries.get("username"));
            String role = String.valueOf(entries.get("role"));
            String nickName = String.valueOf(entries.get("nickName"));
            String creditBalance = String.valueOf(entries.get("creditBalance"));

            if (!StringUtils.hasText(userId)
                    || !StringUtils.hasText(username)
                    || !StringUtils.hasText(role)
                    || !StringUtils.hasText(nickName)
                    || !StringUtils.hasText(creditBalance)) {
                return null;
            }

            return new LoginUser(
                    Long.valueOf(userId),
                    username,
                    role,
                    nickName,
                    Integer.valueOf(creditBalance),
                    token
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return value.substring("Bearer ".length()).trim();
        }
        return value;
    }
}
