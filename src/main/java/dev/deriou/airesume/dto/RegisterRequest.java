package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        @Size(max = 64, message = "username length must be less than or equal to 64")
        String username,

        @NotBlank(message = "password must not be blank")
        @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
        String password,

        @NotBlank(message = "nickName must not be blank")
        @Size(max = 64, message = "nickName length must be less than or equal to 64")
        String nickName,

        @NotBlank(message = "role must not be blank")
        String role,

        @NotBlank(message = "captchaUuid must not be blank")
        String captchaUuid,

        @NotBlank(message = "captchaCode must not be blank")
        String captchaCode
) {
}
