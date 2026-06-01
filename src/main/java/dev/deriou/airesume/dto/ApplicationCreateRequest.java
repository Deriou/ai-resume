package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(
        @NotNull(message = "resumeId must not be null")
        Long resumeId,

        @NotNull(message = "jobId must not be null")
        Long jobId,

        @Size(max = 255, message = "remark length must be less than or equal to 255")
        String remark
) {
}
