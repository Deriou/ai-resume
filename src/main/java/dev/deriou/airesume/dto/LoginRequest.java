package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username must not be blank")
        String username,

        @NotBlank(message = "password must not be blank")
        String password,

        @NotBlank(message = "captchaUuid must not be blank")
        String captchaUuid,

        @NotBlank(message = "captchaCode must not be blank")
        String captchaCode
) {
}
