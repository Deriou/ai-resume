package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeOptimizeRequest(
        @NotBlank(message = "targetDirection must not be blank")
        @Size(max = 64, message = "targetDirection length must be less than or equal to 64")
        String targetDirection
) {
}
