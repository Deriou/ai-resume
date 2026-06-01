package dev.deriou.airesume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobCreateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 128, message = "title length must be less than or equal to 128")
        String title,

        @NotBlank(message = "jdContent must not be blank")
        String jdContent,

        @Size(max = 255, message = "techStack length must be less than or equal to 255")
        String techStack,

        @Size(max = 128, message = "location length must be less than or equal to 128")
        String location
) {
}
