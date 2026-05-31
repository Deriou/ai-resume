package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import dev.deriou.airesume.llm.DeepSeekClient;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/llm")
public class LlmTestController {

    private final DeepSeekClient deepSeekClient;

    public LlmTestController(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    @GetMapping("/hello")
    public ApiResponse<Map<String, String>> hello() {
        String content = deepSeekClient.chat("Reply with one short sentence: AiResume P0 is connected.");
        return ApiResponse.success(Map.of("message", content));
    }
}
