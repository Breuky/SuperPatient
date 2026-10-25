package com.superpatient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.dto.ExtractResultVO;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.exception.BusinessException;
import com.superpatient.extract.DoubleReadReconciler;
import com.superpatient.extract.ExtractionPrompt;
import com.superpatient.extract.IndicatorMatcher;
import com.superpatient.extract.UnitNormalizer;
import com.superpatient.extract.ValueSanityChecker;
import com.superpatient.llm.LlmVisionClient;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 化验单抽取：取图 → 调模型 → 解析 JSON → 归一化 → 返回候选值（不落库）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExtractionService {

    private static final Pattern FILE_URL = Pattern.compile("/api/files/(\\d+)");
    private static final Pattern RANGE = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)\\s*[-~～—]\\s*(-?\\d+(?:\\.\\d+)?)$");
    private static final BigDecimal MAX_REASONABLE = new BigDecimal("1000000");
    /** 有些医院模板把单位和数值挤在一个字段里，如 "6 10^9/L" */
    private static final Pattern VALUE_WITH_UNIT = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)\\s*(\\S.*)$");

    private final FileService fileService;
    private final IndicatorMapper indicatorMapper;
    private final UserMapper userMapper;
    private final ValueMapper valueMapper;
    private final ReferenceRangeService referenceRangeService;
    private final IndicatorMatcher indicatorMatcher;
    private final LlmVisionClient llmVisionClient;
    private final ObjectMapper objectMapper;

    public ExtractResultVO parse(String imageUrl, Integer categoryId) {
        return parse(imageUrl, categoryId, false);
    }

    /**
     * @param doubleRead 精读：同一张图识别两次，只对不一致的行再问一次模型仲裁
     */
    public ExtractResultVO parse(String imageUrl, Integer categoryId, boolean doubleRead) {
        Long fileId = parseFileId(imageUrl);
        byte[] bytes;
        String mimeType;
        try {
            FileService.FileContent content = fileService.load(fileId);
            if (content == null) {
                throw new BusinessException("图片不存在或无权访问");
            }
            mimeType = content.contentType();
            try (InputStream in = content.resource().getInputStream()) {
                bytes = in.readAllBytes();
            }
        } catch (IOException e) {
            throw new BusinessException("读取图片失败: " + e.getMessage());
        }

        List<ExamIndicator> pool = indicatorMapper.findVisibleByCategory(categoryId, CurrentHolder.getCurrentId());
        ReferenceRangeService.UserContext ctx = ReferenceRangeService.UserContext.of(
                userMapper.findById(CurrentHolder.getCurrentId()));
        String prompt = ExtractionPrompt.build(pool);
        Map<Integer, BigDecimal> lastValues = loadLastValues();
        if (doubleRead) {
            return parseDouble(bytes, mimeType, prompt, pool, categoryId, ctx, lastValues);
        }

        String raw = llmVisionClient.extract(bytes, mimeType, prompt);
        log.info("抽取原始输出({}): {}", llmVisionClient.displayName(),
                raw.length() > 500 ? raw.substring(0, 500) + "..." : raw);

        return assemble(raw, pool, categoryId, ctx, lastValues);
    }

    /**
     * 双读：两次调用并行发出 → 逐行比对 → 只对不一致的行做一次带图仲裁。
     * 任一次失败都退化成单读，不让精读把原本可用的结果搞丢。
     */
    private ExtractResultVO parseDouble(byte[] bytes, String mimeType, String prompt,
                                        List<ExamIndicator> pool, Integer categoryId,
                                        ReferenceRangeService.UserContext ctx,
                                        Map<Integer, BigDecimal> lastValues) {
        CompletableFuture<String> first = CompletableFuture.supplyAsync(
                () -> llmVisionClient.extract(bytes, mimeType, prompt));
        CompletableFuture<String> second = CompletableFuture.supplyAsync(
                () -> llmVisionClient.extract(bytes, mimeType, prompt));

        String raw1;
        String raw2;
        try {
            raw1 = first.join();
            raw2 = second.join();
        } catch (Exception e) {
            log.warn("精读的两次调用有一路失败，退化为单读: {}", e.getMessage());
            String single = safeJoin(first, second);
            if (single == null) throw new BusinessException("模型调用失败，请稍后重试");
            return assemble(single, pool, categoryId, ctx, lastValues);
        }

        ExtractResultVO vo1 = safeAssemble(raw1, pool, categoryId, ctx, lastValues);
        ExtractResultVO vo2 = safeAssemble(raw2, pool, categoryId, ctx, lastValues);
        if (vo1 == null && vo2 == null) throw new BusinessException("模型返回的 JSON 无法解析");
        if (vo1 == null) return vo2;
        if (vo2 == null) return vo1;

        List<DoubleReadReconciler.ConflictRow> conflicts = DoubleReadReconciler.findConflicts(vo1, vo2);
        log.info("精读比对完成：{} 行，其中 {} 行两次不一致", vo1.getCandidates().size(), conflicts.size());

        Map<String, DoubleReadReconciler.Arbitration> resolution = Map.of();
        if (!conflicts.isEmpty()) {
            try {
                String arbitrationPrompt = DoubleReadReconciler.buildArbitrationPrompt(conflicts);
                String rawArbitration = llmVisionClient.extract(bytes, mimeType, arbitrationPrompt);
                resolution = DoubleReadReconciler.parseArbitration(rawArbitration, objectMapper);
            } catch (Exception e) {
                log.warn("仲裁调用失败，保留两次读数交由用户判断: {}", e.getMessage());
            }
        }

        ExtractResultVO merged = DoubleReadReconciler.merge(vo1, vo2, resolution);
        applyArbitratedValues(merged, pool);
        return merged;
    }

    /** 仲裁返回的是"图上印的值"，需要按原单位换算后重算异常标记 */
    private void applyArbitratedValues(ExtractResultVO vo, List<ExamIndicator> pool) {
        for (ExtractResultVO.Candidate c : vo.getCandidates()) {
            if (c.getConflict() == null || c.getConflict().getResolved() == null
                    || c.getIndicatorId() == null) {
                continue;
            }
            ExamIndicator indicator = pool.stream()
                    .filter(i -> i.getId().equals(c.getIndicatorId()))
                    .findFirst().orElse(null);
            if (indicator == null) continue;

            BigDecimal value = c.getConflict().getResolved();
            UnitNormalizer.Result unit = UnitNormalizer.normalize(c.getRawUnit(), indicator.getUnit());
            if (unit.converted()) {
                value = value.multiply(unit.factor());
            }
            c.setValueDecimal(value);
            c.setUnit(unit.unit());
            c.setIsAbnormal(calcAbnormal(value, c.getRefMin(), c.getRefMax()));
        }
    }

    private String safeJoin(CompletableFuture<String> first, CompletableFuture<String> second) {
        try {
            return first.isCompletedExceptionally() ? second.join() : first.join();
        } catch (Exception e) {
            try {
                return second.join();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private ExtractResultVO safeAssemble(String raw, List<ExamIndicator> pool, Integer categoryId,
                                         ReferenceRangeService.UserContext ctx,
                                         Map<Integer, BigDecimal> lastValues) {
        try {
            return assemble(raw, pool, categoryId, ctx, lastValues);
        } catch (Exception e) {
            log.warn("精读有一路结果无法解析: {}", e.getMessage());
            return null;
        }
    }

    private ExtractResultVO assemble(String raw, List<ExamIndicator> pool, Integer categoryId,
                                     ReferenceRangeService.UserContext ctx,
                                     Map<Integer, BigDecimal> lastValues) {
        JsonNode root = readJson(raw);

        ExtractResultVO vo = new ExtractResultVO();
        vo.setCategoryId(categoryId);
        vo.setExamDate(text(root, "examDate"));
        vo.setHospital(text(root, "hospital"));
        vo.setEngine(llmVisionClient.displayName());
        vo.setCandidates(new ArrayList<>());
        vo.setUnmatched(new ArrayList<>());

        JsonNode items = root.path("items");
        if (!items.isArray()) {
            throw new BusinessException("模型返回结果缺少 items 数组");
        }

        for (JsonNode item : items) {
            String rawName = text(item, "name");
            String rawValue = text(item, "value");
            String rawUnit = text(item, "unit");

            // 报告里的分组标题（如"白细胞""红细胞"）没有值，不是测量项，直接跳过
            if (parseNumber(rawValue) == null && (rawValue == null || rawValue.isBlank())) {
                log.debug("跳过无值行: {}", rawName);
                continue;
            }

            IndicatorMatcher.Match match = indicatorMatcher.match(rawName, pool);
            if (match == null) {
                ExtractResultVO.Unmatched unmatched = new ExtractResultVO.Unmatched();
                unmatched.setRawName(rawName);

                // 未匹配行同样要把 "0.15 mmol/L" 拆成数值 + 单位，方便人工判断
                String displayValue = rawValue;
                String displayUnit = rawUnit;
                if (rawValue != null) {
                    Matcher vm = VALUE_WITH_UNIT.matcher(rawValue.trim());
                    if (vm.matches()) {
                        displayValue = vm.group(1);
                        if (displayUnit == null || displayUnit.isBlank()) {
                            displayUnit = vm.group(2).trim();
                        }
                    }
                }
                unmatched.setRawValue(displayValue);
                unmatched.setRawUnit(displayUnit);
                unmatched.setRefText(text(item, "refText"));
                vo.getUnmatched().add(unmatched);
                continue;
            }
            vo.getCandidates().add(toCandidate(item, rawName, rawValue, rawUnit, match, ctx, lastValues));
        }
        return vo;
    }

    private ExtractResultVO.Candidate toCandidate(JsonNode item, String rawName, String rawValue,
                                                  String rawUnit, IndicatorMatcher.Match match,
                                                  ReferenceRangeService.UserContext ctx,
                                                  Map<Integer, BigDecimal> lastValues) {
        ExamIndicator indicator = match.indicator();
        List<String> warnings = new ArrayList<>();

        ExtractResultVO.Candidate c = new ExtractResultVO.Candidate();
        c.setRawName(rawName);
        c.setIndicatorId(indicator.getId());
        c.setIndicatorName(indicator.getName());
        c.setRawUnit(rawUnit);
        c.setMatchType(match.matchType());
        c.setConfidence(match.confidence());

        BigDecimal value = parseNumber(rawValue);
        String unitText = rawUnit;

        // 数值解析不出来时，尝试从 "167 g/L" 这类写法里拆出数值（单位单独有值时保留原单位）
        if (value == null && rawValue != null) {
            Matcher vm = VALUE_WITH_UNIT.matcher(rawValue.trim());
            if (vm.matches()) {
                value = new BigDecimal(vm.group(1));
                String unitFromValue = vm.group(2).trim();
                if (rawUnit == null || rawUnit.isBlank()) {
                    unitText = unitFromValue;
                    c.setRawUnit(unitFromValue);
                }
            }
        }

        BigDecimal[] range = resolveRange(item, indicator, ctx);

        if (value == null) {
            c.setValueText(rawValue);
        } else {
            UnitNormalizer.Result unit = UnitNormalizer.normalize(unitText, indicator.getUnit());
            if (unit.converted()) {
                // 数值和参考区间必须一起换算，否则异常判定会错位
                value = value.multiply(unit.factor());
                if (range[0] != null) range[0] = range[0].multiply(unit.factor());
                if (range[1] != null) range[1] = range[1].multiply(unit.factor());
            }
            if (unit.warning() != null) {
                warnings.add(unit.warning());
            }
            c.setValueDecimal(value);
            // 换算成功用指标库单位；无法换算时保留报告原始单位，避免"值 336 却标 pg"
            c.setUnit(unit.unit());
            if (value.signum() < 0 || value.compareTo(MAX_REASONABLE) > 0) {
                warnings.add("数值超出合理范围: " + value.toPlainString());
            }
        }

        c.setRefMin(range[0]);
        c.setRefMax(range[1]);
        c.setRefText(text(item, "refText"));
        c.setIsAbnormal(calcAbnormal(value, range[0], range[1]));

        // 量级 / 基线校验：只提示，不改值
        for (ValueSanityChecker.Finding f : ValueSanityChecker.check(
                value, range[0], range[1], lastValues.get(indicator.getId()))) {
            warnings.add(f.message());
        }

        if (!warnings.isEmpty()) {
            c.setWarning(String.join("；", warnings));
        }
        return c;
    }

    /** 优先用报告印刷的区间，其次按性别/年龄规则，最后回落指标库默认值 */
    private BigDecimal[] resolveRange(JsonNode item, ExamIndicator indicator,
                                      ReferenceRangeService.UserContext ctx) {
        BigDecimal min = decimal(item.get("refMin"));
        BigDecimal max = decimal(item.get("refMax"));
        String refText = text(item, "refText");

        if (min == null && max == null && refText != null) {
            Matcher m = RANGE.matcher(refText.trim());
            if (m.matches()) {
                min = new BigDecimal(m.group(1));
                max = new BigDecimal(m.group(2));
            }
        }
        if (min == null && max == null) {
            ReferenceRangeService.Resolved rule = referenceRangeService.resolve(indicator.getId(), ctx);
            if (rule != null) {
                return new BigDecimal[]{rule.min(), rule.max()};
            }
            return new BigDecimal[]{indicator.getNormalMin(), indicator.getNormalMax()};
        }
        return new BigDecimal[]{min, max};
    }

    private int calcAbnormal(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return 0;
        if (max != null && value.compareTo(max) > 0) return 1;
        if (min != null && value.compareTo(min) < 0) return 2;
        return 0;
    }

    private JsonNode readJson(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```$", "").trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("模型没有返回 JSON");
        }
        try {
            return objectMapper.readTree(s.substring(start, end + 1));
        } catch (Exception e) {
            throw new BusinessException("模型返回的 JSON 无法解析: " + e.getMessage());
        }
    }

    /** 当前用户每个指标最近一次的值，用于基线比对；查不到不影响抽取 */
    private Map<Integer, BigDecimal> loadLastValues() {
        try {
            return valueMapper.findLatestValues(CurrentHolder.getCurrentId()).stream()
                    .filter(v -> v.getIndicatorId() != null && v.getValueDecimal() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            com.superpatient.dto.IndicatorLastValue::getIndicatorId,
                            com.superpatient.dto.IndicatorLastValue::getValueDecimal,
                            (x, y) -> x));
        } catch (Exception e) {
            log.warn("读取历史值失败，跳过基线校验: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long parseFileId(String imageUrl) {
        Matcher m = FILE_URL.matcher(imageUrl == null ? "" : imageUrl);
        if (!m.find()) {
            throw new BusinessException("imageUrl 不合法，应形如 /api/files/12");
        }
        return Long.valueOf(m.group(1));
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.isNumber()) return node.decimalValue();
        return parseNumber(node.asText());
    }

    static BigDecimal parseNumber(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[,，\\s]", "")
                .replaceAll("^[<>≤≥＜＞]=?", "")
                .replaceAll("[↑↓*#]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
