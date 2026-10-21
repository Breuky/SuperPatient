package com.superpatient.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发/演示用：不调用任何外部服务，返回一份固定的血常规抽取结果。
 * 用于在拿到 API Key 之前调通整条链路（含别名归一、单位换算、异常判定）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmVisionClient implements LlmVisionClient {

    private static final String FIXTURE = """
            {
              "examDate": "2026-03-28",
              "hospital": "市第一人民医院",
              "items": [
                {"name": "HGB", "value": "125", "unit": "g/L", "refText": "120-160", "refMin": 120, "refMax": 160},
                {"name": "白细胞计数", "value": "3.2", "unit": "10^9/L", "refText": "4.0-10.0", "refMin": 4, "refMax": 10},
                {"name": "PLT", "value": "90", "unit": "10^9/L", "refText": "100-300", "refMin": 100, "refMax": 300},
                {"name": "CRP", "value": "18.5", "unit": "mg/L", "refText": "0-10", "refMin": 0, "refMax": 10},
                {"name": "葡萄糖", "value": "5.2", "unit": "mmol/L", "refText": "3.9-6.1"},
                {"name": "嗜酸性粒细胞百分比", "value": "2.1", "unit": "%", "refText": "0.4-8.0"}
              ]
            }
            """;

    @Override
    public String extract(byte[] image, String mimeType, String prompt) {
        log.info("Mock 抽取：图片 {} 字节, 提示词 {} 字符", image == null ? 0 : image.length, prompt.length());
        return FIXTURE;
    }

    @Override
    public String displayName() {
        return "mock";
    }
}
