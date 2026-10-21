package com.superpatient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.dto.ExtractResultVO;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.exception.BusinessException;
import com.superpatient.extract.ExtractionPrompt;
import com.superpatient.extract.IndicatorMatcher;
import com.superpatient.extract.UnitNormalizer;
import com.superpatient.llm.LlmVisionClient;
import com.superpatient.mapper.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    private final FileService fileService;
    private final IndicatorMapper indicatorMapper;
    private final IndicatorMatcher indicatorMatcher;
    private final LlmVisionClient llmVisionClient;
    private final ObjectMapper objectMapper;

    public ExtractResultVO parse(String imageUrl, Integer categoryId) {
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

        List<ExamIndicator> pool = indicatorMapper.findByCategoryId(categoryId);
        String prompt = ExtractionPrompt.build(pool);
        String raw = llmVisionClient.extract(bytes, mimeType, prompt);
        log.info("抽取原始输出({}): {}", llmVisionClient.displayName(),
                raw.length() > 500 ? raw.substring(0, 500) + "..." : raw);

        return assemble(raw, pool, categoryId);
    }

    private ExtractResultVO assemble(String raw, List<ExamIndicator> pool, Integer categoryId) {
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

            IndicatorMatcher.Match match = indicatorMatcher.match(rawName, pool);
            if (match == null) {
                ExtractResultVO.Unmatched unmatched = new ExtractResultVO.Unmatched();
                unmatched.setRawName(rawName);
                unmatched.setRawValue(rawValue);
                unmatched.setRawUnit(rawUnit);
                unmatched.setRefText(text(item, "refText"));
                vo.getUnmatched().add(unmatched);
                continue;
            }
            vo.getCandidates().add(toCandidate(item, rawName, rawValue, rawUnit, match));
        }
        return vo;
    }

    private ExtractResultVO.Candidate toCandidate(JsonNode item, String rawName, String rawValue,
                                                  String rawUnit, IndicatorMatcher.Match match) {
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
        BigDecimal[] range = resolveRange(item, indicator);

        if (value == null) {
            c.setValueText(rawValue);
        } else {
            UnitNormalizer.Result unit = UnitNormalizer.normalize(rawUnit, indicator.getUnit());
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
            c.setUnit(indicator.getUnit());
            if (value.signum() < 0 || value.compareTo(MAX_REASONABLE) > 0) {
                warnings.add("数值超出合理范围: " + value.toPlainString());
            }
        }

        c.setRefMin(range[0]);
        c.setRefMax(range[1]);
        c.setRefText(text(item, "refText"));
        c.setIsAbnormal(calcAbnormal(value, range[0], range[1]));

        if (!warnings.isEmpty()) {
            c.setWarning(String.join("；", warnings));
        }
        return c;
    }

    /** 优先用报告印刷的区间，没有再回落指标库默认值 */
    private BigDecimal[] resolveRange(JsonNode item, ExamIndicator indicator) {
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
