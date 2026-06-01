package dev.deriou.airesume.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminCreditGrantRequest(
        @Min(value = 1, message = "amount must be greater than 0")
        Integer amount,

        @Size(max = 255, message = "remark length must be less than or equal to 255")
        String remark
) {
}
