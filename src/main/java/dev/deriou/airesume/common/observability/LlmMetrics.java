package dev.deriou.airesume.common.observability;

import dev.deriou.airesume.llm.LlmResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LlmMetrics {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String TOKEN_TYPE_PROMPT = "prompt";
    private static final String TOKEN_TYPE_COMPLETION = "completion";
    private static final String TOKEN_TYPE_TOTAL = "total";

    private final MeterRegistry meterRegistry;

    public LlmMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(String operation, LlmResult result) {
        String model = normalizeModel(result.model());
        recordCall(operation, STATUS_SUCCESS, model);
        recordTokens(operation, model, result);
        recordLatency(operation, STATUS_SUCCESS, model, result.latencyMs());
    }

    public void recordFailure(String operation, String model, long latencyMs) {
        String safeModel = normalizeModel(model);
        recordCall(operation, STATUS_FAILED, safeModel);
        recordLatency(operation, STATUS_FAILED, safeModel, latencyMs);
    }

    private void recordCall(String operation, String status, String model) {
        Counter.builder("airesume.llm.calls")
                .description("AiResume LLM call count")
                .tag("operation", operation)
                .tag("status", status)
                .tag("model", model)
                .register(meterRegistry)
                .increment();
    }

    private void recordTokens(String operation, String model, LlmResult result) {
        incrementTokens(operation, model, TOKEN_TYPE_PROMPT, result.promptTokens());
        incrementTokens(operation, model, TOKEN_TYPE_COMPLETION, result.completionTokens());
        incrementTokens(operation, model, TOKEN_TYPE_TOTAL, result.totalTokens());
    }

    private void incrementTokens(String operation, String model, String type, int tokens) {
        if (tokens <= 0) {
            return;
        }
        Counter.builder("airesume.llm.tokens")
                .description("AiResume LLM token usage")
                .tag("operation", operation)
                .tag("type", type)
                .tag("model", model)
                .register(meterRegistry)
                .increment(tokens);
    }

    private void recordLatency(String operation, String status, String model, long latencyMs) {
        Timer.builder("airesume.llm.latency")
                .description("AiResume LLM call latency")
                .tag("operation", operation)
                .tag("status", status)
                .tag("model", model)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, latencyMs)));
    }

    private String normalizeModel(String model) {
        return StringUtils.hasText(model) ? model : "unknown";
    }
}
