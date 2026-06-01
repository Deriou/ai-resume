package dev.deriou.airesume.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.vo.ScoreDimensionVO;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LlmJsonParser {

    private static final TypeReference<List<ScoreDimensionVO>> DIMENSION_LIST =
            new TypeReference<>() {
            };
    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public LlmJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedResumeScore parseResumeScore(String content) {
        JsonNode root = readRoot(content);
        int overallScore = score(root, "overallScore");
        List<ScoreDimensionVO> dimensions = convert(required(root, "dimensions"), DIMENSION_LIST);
        List<String> suggestions = convert(required(root, "suggestions"), STRING_LIST);
        return new ParsedResumeScore(overallScore, dimensions, suggestions);
    }

    public ParsedResumeOptimize parseResumeOptimize(String content) {
        JsonNode root = readRoot(content);
        String summary = text(root, "summary");
        List<String> optimizedBullets = convert(required(root, "optimizedBullets"), STRING_LIST);
        List<String> rewriteSuggestions = convert(required(root, "rewriteSuggestions"), STRING_LIST);
        return new ParsedResumeOptimize(summary, optimizedBullets, rewriteSuggestions);
    }

    public ParsedJobMatch parseJobMatch(String content) {
        JsonNode root = readRoot(content);
        int matchScore = score(root, "matchScore");
        List<String> strengths = convert(required(root, "strengths"), STRING_LIST);
        List<String> gaps = convert(required(root, "gaps"), STRING_LIST);
        List<String> suggestions = convert(required(root, "suggestions"), STRING_LIST);
        return new ParsedJobMatch(matchScore, strengths, gaps, suggestions);
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "failed to serialize ai result", ex);
        }
    }

    private JsonNode readRoot(String content) {
        String json = stripCodeFence(content);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new BizException(ResultCode.BIZ_ERROR, "ai response is not a JSON object");
            }
            return root;
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response is not valid JSON", ex);
        }
    }

    private String stripCodeFence(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response is empty");
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd > 0 && lastFence > firstLineEnd) {
            return trimmed.substring(firstLineEnd + 1, lastFence).trim();
        }
        return trimmed;
    }

    private String text(JsonNode root, String field) {
        JsonNode node = required(root, field);
        if (!node.isTextual() || !StringUtils.hasText(node.asText())) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response field " + field + " must be text");
        }
        return node.asText();
    }

    private int score(JsonNode root, String field) {
        JsonNode node = required(root, field);
        if (!node.isInt()) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response field " + field + " must be integer");
        }
        int value = node.asInt();
        if (value < 0 || value > 100) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response field " + field + " must be 0-100");
        }
        return value;
    }

    private JsonNode required(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response missing field " + field);
        }
        return node;
    }

    private <T> T convert(JsonNode node, TypeReference<T> typeReference) {
        if (!node.isArray()) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response field must be array");
        }
        try {
            return objectMapper.convertValue(node, typeReference);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "ai response array format is invalid", ex);
        }
    }

    public record ParsedResumeScore(
            int overallScore,
            List<ScoreDimensionVO> dimensions,
            List<String> suggestions
    ) {
    }

    public record ParsedResumeOptimize(
            String summary,
            List<String> optimizedBullets,
            List<String> rewriteSuggestions
    ) {
    }

    public record ParsedJobMatch(
            int matchScore,
            List<String> strengths,
            List<String> gaps,
            List<String> suggestions
    ) {
    }
}
