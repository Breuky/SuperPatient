package com.superpatient.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通义千问 VL（阿里云百炼 OpenAI 兼容接口）。
 * 用 data URL 传图，要求模型只输出 JSON。
 *
 * 注意：请求体用 Map 构造、响应用字符串接收后自行解析。
 * Spring Boot 4 的 HTTP 消息转换器走 Jackson 3（tools.jackson），
 * 而项目里解析模型输出用的是 Jackson 2（com.fasterxml），
 * 直接把 Jackson 2 的 JsonNode 交给 RestClient 会被序列化成空对象。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "qwen")
public class QwenVlVisionClient implements LlmVisionClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public QwenVlVisionClient(@Value("${app.llm.base-url}") String baseUrl,
                              @Value("${app.llm.api-key:}") String apiKey,
                              @Value("${app.llm.model}") String model,
                              ObjectMapper objectMapper) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("已选择 qwen 抽取，但未配置 app.llm.api-key（环境变量 DASHSCOPE_API_KEY）");
        }
        if (model.isBlank()) {
            throw new IllegalStateException("已选择 qwen 抽取，但未配置 app.llm.model");
        }
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String extract(byte[] image, String mimeType, String prompt) {
        String dataUrl = "data:" + (mimeType == null ? "image/png" : mimeType)
                + ";base64," + Base64.getEncoder().encodeToString(image);

        Map<String, Object> textPart = Map.of("type", "text", "text", prompt);
        Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(textPart, imagePart))));

        String raw;
        long start = System.currentTimeMillis();
        try {
            raw = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("调用 Qwen-VL 失败（耗时 {}ms）", System.currentTimeMillis() - start, e);
            throw new BusinessException("模型调用失败，请稍后重试");
        }
        log.info("Qwen-VL 调用完成，耗时 {}ms，响应 {} 字符",
                System.currentTimeMillis() - start, raw == null ? 0 : raw.length());

        return extractContent(raw);
    }

    /**
     * 兼容两种返回：content 直接是字符串，或是由若干 {type:text,text:...} 组成的数组。
     */
    private String extractContent(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("模型未返回内容");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            log.error("解析模型响应失败: {}", raw.substring(0, Math.min(300, raw.length())), e);
            throw new BusinessException("模型响应格式异常");
        }

        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String message = error.path("message").asText("unknown");
            log.error("模型返回错误: {}", message);
            throw new BusinessException("模型返回错误: " + message);
        }

        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                if (part.has("text")) sb.append(part.path("text").asText());
            }
            if (sb.length() > 0) return sb.toString();
        }
        throw new BusinessException("模型未返回内容");
    }

    @Override
    public String displayName() {
        return model;
    }
}
