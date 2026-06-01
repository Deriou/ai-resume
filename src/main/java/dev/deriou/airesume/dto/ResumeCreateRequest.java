package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeCreateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 128, message = "title length must be less than or equal to 128")
        String title,

        @NotBlank(message = "contentMd must not be blank")
        String contentMd
) {
}
