package com.superpatient.service;

import com.superpatient.dto.InsightRequest;
import com.superpatient.dto.InsightVO;
import com.superpatient.dto.RecordDetailVO;
import com.superpatient.dto.ValueVO;
import com.superpatient.entity.ExamValue;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告解读：把结构化证据交给 Python Agent 服务，模型不可用时降级为规则摘要。
 *
 * 边界：Java 负责取数、鉴权、免责声明与降级；Python 只负责 prompt 编排与工具调用。
 */
@Slf4j
@Service
public class InsightService {

    private static final String DISCLAIMER = "以上内容仅是本系统对历史数据的描述，不构成医疗建议；如有疑问请咨询医生。";

    private final RecordService recordService;
    private final ValueMapper valueMapper;
    private final RestClient restClient;
    private final boolean enabled;
    private final String model;
    private final String apiKey;
    private final String javaBase;

    public InsightService(RecordService recordService,
                          ValueMapper valueMapper,
                          @Value("${app.agent.enabled:false}") boolean enabled,
                          @Value("${app.agent.base-url:http://127.0.0.1:8001}") String baseUrl,
                          @Value("${app.agent.model:qwen-plus}") String model,
                          @Value("${app.llm.api-key:}") String apiKey,
                          @Value("${server.port:8080}") int serverPort) {
        this.recordService = recordService;
        this.valueMapper = valueMapper;
        this.enabled = enabled;
        this.model = model;
        this.apiKey = apiKey;
        this.javaBase = "http://127.0.0.1:" + serverPort;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(40_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public InsightVO explain(InsightRequest request) {
        if (request.getRecordId() == null) {
            throw new BusinessException("recordId 不能为空");
        }
        RecordDetailVO record = recordService.detail(request.getRecordId());
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        boolean agentMode = "agent".equalsIgnoreCase(request.getMode());
        if (!enabled) {
            return fallback(record, "未启用 Agent 服务");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recordId", request.getRecordId());
            payload.put("question", request.getQuestion());
            payload.put("javaBase", javaBase);
            if (!agentMode) {
                payload.put("evidence", buildEvidence(record));
            }

            String path = agentMode ? "/agent/report-insight" : "/agent/summarize";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(path)
                    .header("X-User-Token", currentToken())
                    .header("X-Llm-Key", apiKey)
                    .header("X-Llm-Model", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                String error = response == null ? "无响应" : String.valueOf(response.get("error"));
                return fallback(record, error);
            }
            return toVO(response);
        } catch (Exception e) {
            log.warn("Agent 服务调用失败，降级为规则摘要: {}", e.getMessage());
            return fallback(record, e.getMessage());
        }
    }

    /** 证据包：这次检查 + 异常项的历史序列，供 simple 模式一次性生成 */
    private Map<String, Object> buildEvidence(RecordDetailVO record) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("examDate", record.getExamDate());
        evidence.put("hospital", record.getHospital());
        evidence.put("category", record.getCategoryName());

        List<Map<String, Object>> values = new ArrayList<>();
        int abnormalCount = 0;
        for (ValueVO v : record.getValues()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("indicator", v.getIndicatorName());
            item.put("value", v.getValueDecimal() != null ? v.getValueDecimal() : v.getValueText());
            item.put("unit", v.getUnit());
            item.put("ref_min", v.getRefMin());
            item.put("ref_max", v.getRefMax());
            item.put("is_abnormal", v.getIsAbnormal());
            values.add(item);
            if (v.getIsAbnormal() != null && v.getIsAbnormal() != 0) {
                abnormalCount++;
                item.put("history", recentHistory(v.getIndicatorId()));
            }
        }
        evidence.put("abnormal_count", abnormalCount);
        evidence.put("values", values);
        return evidence;
    }

    private List<Map<String, Object>> recentHistory(Integer indicatorId) {
        List<ExamValue> values = valueMapper.findByUserIdAndIndicatorId(
                CurrentHolder.getCurrentId(), indicatorId, null, null);
        int from = Math.max(0, values.size() - 6);
        List<Map<String, Object>> points = new ArrayList<>();
        for (ExamValue v : values.subList(from, values.size())) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", v.getExamDate() != null
                    ? v.getExamDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            p.put("value", v.getValueDecimal());
            p.put("is_abnormal", v.getIsAbnormal());
            points.add(p);
        }
        return points;
    }

    /** 模型不可用时的确定性版本，保证功能不依赖 Python 服务 */
    private InsightVO fallback(RecordDetailVO record, String reason) {
        InsightVO vo = new InsightVO();
        vo.setMode("rule");
        vo.setDegraded(true);
        vo.setDisclaimer(DISCLAIMER);

        List<InsightVO.Highlight> highlights = new ArrayList<>();
        for (ValueVO v : record.getValues()) {
            if (v.getIsAbnormal() == null || v.getIsAbnormal() == 0) continue;
            InsightVO.Highlight h = new InsightVO.Highlight();
            h.setIndicator(v.getIndicatorName());
            String direction = v.getIsAbnormal() == 1 ? "高于" : "低于";
            String bound = v.getIsAbnormal() == 1 ? "上限 " + v.getRefMax() : "下限 " + v.getRefMin();
            h.setText(v.getValueDecimal() + " " + (v.getUnit() == null ? "" : v.getUnit())
                    + "，" + direction + "参考区间" + bound);
            h.setBasis("本次报告");
            highlights.add(h);
        }
        vo.setHighlights(highlights);
        vo.setSummary(highlights.isEmpty()
                ? "本次 " + record.getValues().size() + " 项指标均在参考区间内"
                : "本次共 " + record.getValues().size() + " 项，其中 " + highlights.size() + " 项超出参考区间");
        if (reason != null && !reason.isBlank()) {
            vo.getWarnings().add("已降级为规则摘要：" + reason);
        }
        return vo;
    }

    @SuppressWarnings("unchecked")
    private InsightVO toVO(Map<String, Object> response) {
        InsightVO vo = new InsightVO();
        vo.setMode(String.valueOf(response.getOrDefault("mode", "simple")));
        vo.setSummary(String.valueOf(response.getOrDefault("summary", "")));
        vo.setDisclaimer(DISCLAIMER);
        vo.setDegraded(Boolean.TRUE.equals(response.get("degraded")));
        Object turns = response.get("turns");
        if (turns instanceof Number) vo.setTurns(((Number) turns).intValue());

        for (Object item : (List<Object>) response.getOrDefault("highlights", List.of())) {
            Map<String, Object> m = (Map<String, Object>) item;
            InsightVO.Highlight h = new InsightVO.Highlight();
            h.setIndicator((String) m.get("indicator"));
            h.setText((String) m.get("text"));
            h.setBasis((String) m.get("basis"));
            vo.getHighlights().add(h);
        }
        for (Object item : (List<Object>) response.getOrDefault("warnings", List.of())) {
            vo.getWarnings().add(String.valueOf(item));
        }
        for (Object item : (List<Object>) response.getOrDefault("trace", List.of())) {
            Map<String, Object> m = (Map<String, Object>) item;
            InsightVO.TraceItem t = new InsightVO.TraceItem();
            t.setTool((String) m.get("tool"));
            t.setArgs((Map<String, Object>) m.get("args"));
            t.setMs(m.get("ms") instanceof Number ? ((Number) m.get("ms")).intValue() : null);
            t.setOk(Boolean.TRUE.equals(m.get("ok")));
            vo.getTrace().add(t);
        }
        return vo;
    }

    private String currentToken() {
        return CurrentHolder.getToken();
    }
}
