package dev.deriou.airesume.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.context.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (UserHolder.current().isPresent()) {
            return true;
        }

        response.setStatus(ResultCode.UNAUTHORIZED.getHttpStatus());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getDefaultMessage())
        ));
        return false;
    }
}
