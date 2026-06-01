package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationReviewRequest(
        @NotBlank(message = "status must not be blank")
        String status,

        @Size(max = 255, message = "remark length must be less than or equal to 255")
        String remark
) {
}
