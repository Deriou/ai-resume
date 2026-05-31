package dev.deriou.airesume.llm.dto;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        boolean stream
) {
}
