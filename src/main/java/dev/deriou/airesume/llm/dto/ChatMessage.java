package dev.deriou.airesume.llm.dto;

public record ChatMessage(
        String role,
        String content
) {
}
