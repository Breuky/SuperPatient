package com.superpatient.service;

import com.superpatient.dto.InsightRequest;
import com.superpatient.dto.InsightVO;
import com.superpatient.dto.RecordDetailVO;
import com.superpatient.dto.ValueVO;
import com.superpatient.entity.ExamValue;
import com.superpatient.exception.BusinessException;
import com.superpatient.entity.InsightLog;
import com.superpatient.mapper.InsightLogMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import com.superpatient.util.InsightClassifier;
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
    private final InsightLogMapper insightLogMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final RestClient restClient;
    private final boolean enabled;
    private final String model;
    private final String apiKey;
    private final String javaBase;

    public InsightService(RecordService recordService,
                          ValueMapper valueMapper,
                          InsightLogMapper insightLogMapper,
                          com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                          @Value("${app.agent.enabled:false}") boolean enabled,
                          @Value("${app.agent.base-url:http://127.0.0.1:8001}") String baseUrl,
                          @Value("${app.agent.model:qwen-plus}") String model,
                          @Value("${app.llm.api-key:}") String apiKey,
                          @Value("${server.port:8080}") int serverPort) {
        this.recordService = recordService;
        this.valueMapper = valueMapper;
        this.insightLogMapper = insightLogMapper;
        this.objectMapper = objectMapper;
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

        long started = System.currentTimeMillis();
        // 默认走深读：simple 只是模型/服务异常时的兜底，不该是主路径
        boolean agentMode = request.getMode() == null || request.getMode().isBlank()
                || "agent".equalsIgnoreCase(request.getMode());
        InsightVO result;
        if (!enabled) {
            result = fallback(record, "未启用 Agent 服务");
            logInvocation(request, result, started);
            return result;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recordId", request.getRecordId());
            payload.put("question", request.getQuestion());
            payload.put("javaBase", javaBase);
            payload.put("history", trimmedHistory(request.getHistory()));
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
                result = fallback(record, error);
            } else {
                result = toVO(response);
            }
        } catch (Exception e) {
            log.warn("Agent 服务调用失败，降级为规则摘要: {}", e.getMessage());
            result = fallback(record, e.getMessage());
        }
        logInvocation(request, result, started);
        return result;
    }

    /** 留痕用于观察真实使用：问了什么、查了几次工具、花了多少 token。失败不影响主流程 */
    private void logInvocation(InsightRequest request, InsightVO vo, long started) {
        try {
            InsightLog entry = new InsightLog();
            entry.setUserId(CurrentHolder.getCurrentId());
            entry.setRecordId(request.getRecordId());
            entry.setMode(vo.getMode());
            entry.setQuestion(request.getQuestion());
            entry.setCategory(InsightClassifier.classify(request.getQuestion()));
            entry.setTurns(vo.getTurns());
            entry.setToolCalls(vo.getTrace() == null ? 0 : vo.getTrace().size());
            entry.setTokens(vo.getUsage() == null ? 0 : intOf(vo.getUsage().get("total_tokens")));
            entry.setElapsedMs((int) (System.currentTimeMillis() - started));
            entry.setDegraded(vo.isDegraded() ? 1 : 0);
            entry.setSummary(truncate(vo.getSummary(), 1000));
            if (vo.getTrace() != null && !vo.getTrace().isEmpty()) {
                entry.setTraceJson(objectMapper.writeValueAsString(vo.getTrace()));
            }
            insightLogMapper.insert(entry);
        } catch (Exception e) {
            log.debug("记录 Agent 调用留痕失败: {}", e.getMessage());
        }
    }

    public List<com.superpatient.dto.InsightStatVO> categoryStats(Long userId) {
        return insightLogMapper.statsByCategory(userId);
    }

    public List<InsightLog> recentLogs(Long userId, Integer limit) {
        return insightLogMapper.findRecent(userId, limit == null ? 20 : Math.min(limit, 100));
    }

    private static int intOf(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * 随访提醒的文案润色：一次请求批量处理，失败就返回空 map（前端回落到 reason）。
     */
    public Map<String, String> followUpNotes(List<Map<String, Object>> items) {
        Map<String, String> notes = new LinkedHashMap<>();
        if (!enabled || items == null || items.isEmpty() || apiKey.isBlank()) {
            return notes;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/agent/followup-note")
                    .header("X-Llm-Key", apiKey)
                    .header("X-Llm-Model", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("items", items))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                return notes;
            }
            for (Object item : (List<Object>) response.getOrDefault("notes", List.of())) {
                Map<String, Object> m = (Map<String, Object>) item;
                Object indicator = m.get("indicator");
                Object note = m.get("note");
                if (indicator != null && note != null) {
                    notes.put(String.valueOf(indicator), String.valueOf(note));
                }
            }
        } catch (Exception e) {
            log.warn("提醒文案生成失败，使用规则文案: {}", e.getMessage());
        }
        return notes;
    }

    /**
     * 对话历史：只带最近 8 轮，每轮截断到 600 字。
     * 追问需要上下文，但把整段对话灌进去既贵又容易让模型照着旧结论编。
     */
    static List<Map<String, String>> trimmedHistory(List<InsightRequest.ChatTurn> history) {
        if (history == null || history.isEmpty()) return List.of();
        List<InsightRequest.ChatTurn> tail = history.subList(Math.max(0, history.size() - 8), history.size());
        List<Map<String, String>> turns = new ArrayList<>();
        for (InsightRequest.ChatTurn turn : tail) {
            if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) continue;
            String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "assistant" : "user";
            String content = turn.getContent().trim();
            turns.add(Map.of("role", role,
                    "content", content.length() > 600 ? content.substring(0, 600) : content));
        }
        return turns;
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
        vo.setUsage((Map<String, Object>) response.get("usage"));

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
