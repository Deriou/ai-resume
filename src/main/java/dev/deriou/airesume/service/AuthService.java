package dev.deriou.airesume.service;

import dev.deriou.airesume.dto.CaptchaResponse;
import dev.deriou.airesume.dto.LoginRequest;
import dev.deriou.airesume.dto.RegisterRequest;
import dev.deriou.airesume.vo.LoginResponse;

public interface AuthService {

    CaptchaResponse captcha();

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse me();

    void logout(String authorization);
}
