package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotNull;

public record JobMatchRequest(
        @NotNull(message = "resumeId must not be null")
        Long resumeId
) {
}
