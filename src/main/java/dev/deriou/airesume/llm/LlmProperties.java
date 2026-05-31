package dev.deriou.airesume.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "airesume.llm.deepseek")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration timeout
) {
}
