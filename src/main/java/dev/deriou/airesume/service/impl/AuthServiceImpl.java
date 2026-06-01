package dev.deriou.airesume.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.UserHolder;
import dev.deriou.airesume.dto.CaptchaResponse;
import dev.deriou.airesume.dto.LoginRequest;
import dev.deriou.airesume.dto.RegisterRequest;
import dev.deriou.airesume.entity.User;
import dev.deriou.airesume.mapper.UserMapper;
import dev.deriou.airesume.redis.RedisKeys;
import dev.deriou.airesume.service.AuthService;
import dev.deriou.airesume.vo.LoginResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int INITIAL_CREDIT_BALANCE = 20;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private static final String ENTERPRISE_ROLE = "ENTERPRISE";

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthServiceImpl(UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CaptchaResponse captcha() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode().toLowerCase(Locale.ROOT);
        stringRedisTemplate.opsForValue().set(RedisKeys.captcha(uuid), code, RedisKeys.CAPTCHA_TTL);

        String imageBase64 = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(captcha.getImageBytes());
        return new CaptchaResponse(uuid, imageBase64);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateCaptcha(request.captchaUuid(), request.captchaCode());
        String role = normalizeRegisterRole(request.role());
        ensureUsernameAvailable(request.username());

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(BCrypt.hashpw(request.password()));
        user.setRole(role);
        user.setNickName(request.nickName().trim());
        user.setCreditBalance(INITIAL_CREDIT_BALANCE);
        user.setStatus(ACTIVE_STATUS);
        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        validateCaptcha(request.captchaUuid(), request.captchaCode());
        User user = findByUsername(request.username());
        if (user == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "username or password is incorrect");
        }
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "user is disabled");
        }
        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw new BizException(ResultCode.BIZ_ERROR, "username or password is incorrect");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        LoginResponse response = toLoginResponse(token, user);
        stringRedisTemplate.opsForHash().putAll(RedisKeys.loginToken(token), toRedisHash(response));
        stringRedisTemplate.expire(RedisKeys.loginToken(token), RedisKeys.LOGIN_TOKEN_TTL);
        return response;
    }

    @Override
    public LoginResponse me() {
        LoginUser loginUser = UserHolder.current()
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED));
        User user = userMapper.selectById(loginUser.userId());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return new LoginResponse(
                loginUser.token(),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickName(),
                user.getCreditBalance()
        );
    }

    @Override
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (StringUtils.hasText(token)) {
            stringRedisTemplate.delete(RedisKeys.loginToken(token));
        }
        UserHolder.remove();
    }

    private void validateCaptcha(String uuid, String code) {
        String key = RedisKeys.captcha(uuid);
        String cachedCode = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);

        if (!StringUtils.hasText(cachedCode)) {
            throw new BizException(ResultCode.BIZ_ERROR, "captcha expired");
        }
        if (!cachedCode.equals(code.trim().toLowerCase(Locale.ROOT))) {
            throw new BizException(ResultCode.BIZ_ERROR, "captcha is incorrect");
        }
    }

    private String normalizeRegisterRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (ADMIN_ROLE.equals(normalized)) {
            throw new BizException(ResultCode.FORBIDDEN, "admin registration is not allowed");
        }
        if (!USER_ROLE.equals(normalized) && !ENTERPRISE_ROLE.equals(normalized)) {
            throw new BizException(ResultCode.BIZ_ERROR, "role must be USER or ENTERPRISE");
        }
        return normalized;
    }

    private void ensureUsernameAvailable(String username) {
        if (findByUsername(username) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "username already exists");
        }
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username.trim())
                .last("LIMIT 1"));
    }

    private LoginResponse toLoginResponse(String token, User user) {
        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickName(),
                user.getCreditBalance()
        );
    }

    private Map<String, String> toRedisHash(LoginResponse response) {
        Map<String, String> hash = new HashMap<>();
        hash.put("userId", String.valueOf(response.userId()));
        hash.put("username", response.username());
        hash.put("role", response.role());
        hash.put("nickName", response.nickName());
        hash.put("creditBalance", String.valueOf(response.creditBalance()));
        return hash;
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
