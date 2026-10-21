package com.superpatient.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.superpatient.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;

/**
 * 通义千问 VL（阿里云百炼 OpenAI 兼容接口）。
 * 用 data URL 传图，要求模型只输出 JSON。
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
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String extract(byte[] image, String mimeType, String prompt) {
        String dataUrl = "data:" + (mimeType == null ? "image/png" : mimeType)
                + ";base64," + Base64.getEncoder().encodeToString(image);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);

        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        content.addObject().put("type", "image_url")
                .putObject("image_url").put("url", dataUrl);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("调用 Qwen-VL 失败", e);
            throw new BusinessException("模型调用失败，请稍后重试");
        }

        JsonNode text = response == null ? null
                : response.path("choices").path(0).path("message").path("content");
        if (text == null || text.isMissingNode() || text.asText().isBlank()) {
            throw new BusinessException("模型未返回内容");
        }
        return text.asText();
    }

    @Override
    public String displayName() {
        return model;
    }
}
