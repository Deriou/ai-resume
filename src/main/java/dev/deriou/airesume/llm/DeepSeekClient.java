package dev.deriou.airesume.llm;

import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.llm.dto.ChatCompletionRequest;
import dev.deriou.airesume.llm.dto.ChatCompletionResponse;
import dev.deriou.airesume.llm.dto.ChatMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DeepSeekClient {

    private final RestClient restClient;
    private final LlmProperties properties;

    public DeepSeekClient(RestClient deepSeekRestClient, LlmProperties properties) {
        this.restClient = deepSeekRestClient;
        this.properties = properties;
    }

    public String chat(String prompt) {
        return chatWithUsage(prompt).content();
    }

    public LlmResult chatWithUsage(String prompt) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new BizException(ResultCode.BIZ_ERROR, "DEEPSEEK_API_KEY is not configured");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(new ChatMessage("user", prompt)),
                false
        );

        Instant start = Instant.now();
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new BizException(
                                ResultCode.BIZ_ERROR,
                                "DeepSeek request failed: HTTP " + httpResponse.getStatusCode().value()
                        );
                    })
                    .body(ChatCompletionResponse.class);

            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return toResult(response, latencyMs);
        } catch (BizException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "DeepSeek request failed", ex);
        }
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "DeepSeek returned empty response");
        }

        ChatCompletionResponse.Choice firstChoice = response.choices().getFirst();
        if (firstChoice.message() == null || !StringUtils.hasText(firstChoice.message().content())) {
            throw new BizException(ResultCode.BIZ_ERROR, "DeepSeek returned empty content");
        }

        return firstChoice.message().content();
    }

    private LlmResult toResult(ChatCompletionResponse response, long latencyMs) {
        String content = extractContent(response);
        ChatCompletionResponse.Usage usage = response.usage();
        return new LlmResult(
                content,
                StringUtils.hasText(response.model()) ? response.model() : properties.model(),
                usage != null && usage.promptTokens() != null ? usage.promptTokens() : 0,
                usage != null && usage.completionTokens() != null ? usage.completionTokens() : 0,
                usage != null && usage.totalTokens() != null ? usage.totalTokens() : 0,
                latencyMs
        );
    }
}
