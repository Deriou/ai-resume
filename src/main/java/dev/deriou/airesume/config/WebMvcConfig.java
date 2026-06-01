package dev.deriou.airesume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.deriou.airesume.interceptor.AuthInterceptor;
import dev.deriou.airesume.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public WebMvcConfig(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(new AuthInterceptor(objectMapper))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/captcha",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/health",
                        "/api/dev/**",
                        "/actuator/**"
                )
                .order(1);
    }
}
