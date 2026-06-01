package dev.deriou.airesume.dto;

public record CaptchaResponse(
        String uuid,
        String imageBase64
) {
}
