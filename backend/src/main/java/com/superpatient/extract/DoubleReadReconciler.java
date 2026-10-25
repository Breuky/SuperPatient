package com.superpatient.extract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.dto.ExtractResultVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 精读（双读）结果的比对与合并。
 *
 * 同一张图识别两次，逐行比对：一致的行直接采用，不一致的行交给模型仲裁
 * （结论可以是"看不清楚"，此时保留两次读数让用户判断）。
 *
 * 这个类只做纯逻辑，不发请求、不碰数据库，便于单测。
 */
public final class DoubleReadReconciler {

    /** 两次读数的相对容差，抵消 3.5 与 3.50 这类写法差异 */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.005");

    private DoubleReadReconciler() {
    }

    /** 需要仲裁的一行 */
    public record ConflictRow(String key, String label, BigDecimal first, BigDecimal second,
                              String firstText, String secondText,
                              String unit, String refText) {

        boolean isText() {
            return first == null && second == null;
        }

        String displayFirst() {
            return first != null ? first.toPlainString() : firstText;
        }

        String displaySecond() {
            return second != null ? second.toPlainString() : secondText;
        }
    }

    /** 仲裁结论，value/reason 都可能为空（模型看不清） */
    public record Arbitration(BigDecimal value, String reason) {
    }

    /** 行主键：已匹配用指标 id，未匹配用规范化原文名 + 单位 */
    public static String key(ExtractResultVO.Candidate c) {
        if (c.getIndicatorId() != null) {
            return "id:" + c.getIndicatorId();
        }
        return "raw:" + IndicatorMatcher.normalize(Objects.toString(c.getRawName(), ""))
                + "|" + Objects.toString(c.getRawUnit(), "").trim();
    }

    public static String key(ExtractResultVO.Unmatched u) {
        return "raw:" + IndicatorMatcher.normalize(Objects.toString(u.getRawName(), ""))
                + "|" + Objects.toString(u.getRawUnit(), "").trim();
    }

    /** 找出两次读数不一致的行（含"一次有一行、另一次没有"），匹配与未匹配都算 */
    public static List<ConflictRow> findConflicts(ExtractResultVO a, ExtractResultVO b) {
        List<ConflictRow> conflicts = findCandidateConflicts(a, b);
        conflicts.addAll(findUnmatchedConflicts(a, b));
        return conflicts;
    }

    private static List<ConflictRow> findCandidateConflicts(ExtractResultVO a, ExtractResultVO b) {
        Map<String, ExtractResultVO.Candidate> first = indexCandidates(a);
        Map<String, ExtractResultVO.Candidate> second = indexCandidates(b);

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(first.keySet());
        keys.addAll(second.keySet());

        List<ConflictRow> conflicts = new ArrayList<>();
        for (String key : keys) {
            ExtractResultVO.Candidate x = first.get(key);
            ExtractResultVO.Candidate y = second.get(key);
            if (x != null && y != null && sameRead(x, y)) {
                continue;
            }
            ExtractResultVO.Candidate sample = x != null ? x : y;
            conflicts.add(new ConflictRow(
                    key,
                    Objects.toString(sample.getIndicatorName(), sample.getRawName()),
                    x == null ? null : x.getValueDecimal(),
                    y == null ? null : y.getValueDecimal(),
                    x == null ? null : x.getValueText(),
                    y == null ? null : y.getValueText(),
                    Objects.toString(sample.getUnit(), sample.getRawUnit()),
                    sample.getRefText()));
        }
        return conflicts;
    }

    /**
     * 合并两次结果。resolution 为空表示该行没有仲裁结论，此时保留第一次读到的值，
     * 并在 conflict 里同时给出两个值。
     */
    public static ExtractResultVO merge(ExtractResultVO a, ExtractResultVO b,
                                        Map<String, Arbitration> resolution) {
        Map<String, ExtractResultVO.Candidate> first = indexCandidates(a);
        Map<String, ExtractResultVO.Candidate> second = indexCandidates(b);

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(first.keySet());
        keys.addAll(second.keySet());

        List<ExtractResultVO.Candidate> merged = new ArrayList<>();
        for (String key : keys) {
            ExtractResultVO.Candidate x = first.get(key);
            ExtractResultVO.Candidate y = second.get(key);
            ExtractResultVO.Candidate chosen = x != null ? x : y;

            if (x != null && y != null && sameRead(x, y)) {
                merged.add(chosen);
                continue;
            }

            Arbitration arb = resolution == null ? null : resolution.get(key);
            if (arb != null && arb.value() != null) {
                chosen.setValueDecimal(arb.value());
                chosen.setValueText(null);
            }

            ExtractResultVO.Conflict conflict = new ExtractResultVO.Conflict();
            conflict.setFirst(x == null ? null : x.getValueDecimal());
            conflict.setSecond(y == null ? null : y.getValueDecimal());
            conflict.setResolved(arb == null ? null : arb.value());
            conflict.setReason(arb == null ? null : arb.reason());
            conflict.setAgreement(false);
            conflict.setMissingInOneRead(x == null || y == null);
            chosen.setConflict(conflict);

            if (arb != null && arb.value() != null) {
                chosen.setWarning(joinWarning(chosen.getWarning(), "精读仲裁：" + arb.reason()));
            } else {
                chosen.setWarning(joinWarning(chosen.getWarning(),
                        "两次识别不一致（" + text(x, y) + "），请核对原图"));
            }
            merged.add(chosen);
        }

        ExtractResultVO result = new ExtractResultVO();
        result.setCategoryId(a.getCategoryId());
        result.setExamDate(a.getExamDate() != null ? a.getExamDate() : b.getExamDate());
        result.setHospital(a.getHospital() != null ? a.getHospital() : b.getHospital());
        result.setEngine(a.getEngine() + " ×2");
        result.setCandidates(merged);
        result.setUnmatched(mergeUnmatched(a.getUnmatched(), b.getUnmatched(), resolution));
        return result;
    }

    /** 未匹配项两次读数不同也算冲突——它们同样会影响用户后续建档与选值 */
    private static List<ConflictRow> findUnmatchedConflicts(ExtractResultVO a, ExtractResultVO b) {
        Map<String, ExtractResultVO.Unmatched> first = indexUnmatched(a);
        Map<String, ExtractResultVO.Unmatched> second = indexUnmatched(b);

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(first.keySet());
        keys.addAll(second.keySet());

        List<ConflictRow> conflicts = new ArrayList<>();
        for (String key : keys) {
            ExtractResultVO.Unmatched x = first.get(key);
            ExtractResultVO.Unmatched y = second.get(key);
            if (x != null && y != null && Objects.equals(trim(x.getRawValue()), trim(y.getRawValue()))) {
                continue;
            }
            ExtractResultVO.Unmatched sample = x != null ? x : y;
            conflicts.add(new ConflictRow(key, sample.getRawName(),
                    parseValue(x == null ? null : x.getRawValue()),
                    parseValue(y == null ? null : y.getRawValue()),
                    x == null ? null : x.getRawValue(),
                    y == null ? null : y.getRawValue(),
                    sample.getRawUnit(), sample.getRefText()));
        }
        return conflicts;
    }

    /** 未匹配项：合并去重；有仲裁结论时用仲裁值覆盖原文值 */
    private static List<ExtractResultVO.Unmatched> mergeUnmatched(List<ExtractResultVO.Unmatched> a,
                                                                  List<ExtractResultVO.Unmatched> b,
                                                                  Map<String, Arbitration> resolution) {
        Map<String, ExtractResultVO.Unmatched> merged = new LinkedHashMap<>();
        if (a != null) {
            for (ExtractResultVO.Unmatched u : a) {
                merged.putIfAbsent(key(u), u);
            }
        }
        if (b != null) {
            for (ExtractResultVO.Unmatched u : b) {
                merged.putIfAbsent(key(u), u);
            }
        }
        if (resolution != null && !resolution.isEmpty()) {
            merged.forEach((key, u) -> {
                Arbitration arb = resolution.get(key);
                if (arb != null && arb.value() != null) {
                    u.setRawValue(arb.value().toPlainString());
                }
            });
        }
        return new ArrayList<>(merged.values());
    }

    private static Map<String, ExtractResultVO.Unmatched> indexUnmatched(ExtractResultVO vo) {
        Map<String, ExtractResultVO.Unmatched> map = new LinkedHashMap<>();
        if (vo == null || vo.getUnmatched() == null) return map;
        for (ExtractResultVO.Unmatched u : vo.getUnmatched()) {
            map.putIfAbsent(key(u), u);
        }
        return map;
    }

    /** "<0.04" "3.2↑" 这类原文清洗成数值，解析不出就返回 null */
    static BigDecimal parseValue(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim()
                .replaceAll("[,，\\s]", "")
                .replaceAll("^[<>≤≥＜＞]=?", "")
                .replaceAll("[↑↓*#]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(ExtractResultVO.Candidate x, ExtractResultVO.Candidate y) {
        String first = x == null ? "未识别到" : valueOf(x);
        String second = y == null ? "未识别到" : valueOf(y);
        return first + " / " + second;
    }

    private static String valueOf(ExtractResultVO.Candidate c) {
        return c.getValueDecimal() != null ? c.getValueDecimal().toPlainString() : c.getValueText();
    }

    private static String joinWarning(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + "；" + addition;
    }

    private static Map<String, ExtractResultVO.Candidate> indexCandidates(ExtractResultVO vo) {
        Map<String, ExtractResultVO.Candidate> map = new LinkedHashMap<>();
        if (vo == null || vo.getCandidates() == null) return map;
        for (ExtractResultVO.Candidate c : vo.getCandidates()) {
            map.putIfAbsent(key(c), c);
        }
        return map;
    }

    private static boolean sameRead(ExtractResultVO.Candidate x, ExtractResultVO.Candidate y) {
        boolean xNum = x.getValueDecimal() != null;
        boolean yNum = y.getValueDecimal() != null;
        if (xNum && yNum) {
            return closeEnough(x.getValueDecimal(), y.getValueDecimal());
        }
        if (!xNum && !yNum) {
            return Objects.equals(trim(x.getValueText()), trim(y.getValueText()));
        }
        return false;
    }

    static boolean closeEnough(BigDecimal x, BigDecimal y) {
        if (x == null || y == null) return x == y;
        if (x.compareTo(y) == 0) return true;
        BigDecimal diff = x.subtract(y).abs();
        BigDecimal base = x.abs().max(y.abs()).max(BigDecimal.ONE);
        return diff.compareTo(base.multiply(TOLERANCE)) <= 0;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /* ---------------- 仲裁提示词与结果解析 ---------------- */

    public static String buildArbitrationPrompt(List<ConflictRow> conflicts) {
        StringBuilder rows = new StringBuilder();
        for (ConflictRow c : conflicts) {
            rows.append("- key=").append(c.key())
                .append(" | 指标=").append(c.label())
                .append(" | 第一次=").append(c.displayFirst())
                .append(" | 第二次=").append(c.displaySecond())
                .append(" | 单位=").append(Objects.toString(c.unit(), "-"))
                .append(" | 参考区间=").append(Objects.toString(c.refText(), "-"))
                .append("\n");
        }
        return """
                你在核对一张化验单的两次识别结果。下面这些行两次读数不同。

                请重新看图，对每一行给出你认为正确的值：
                - 只输出图上真实印着的值，不要推算、不要补位
                - 单位换算不需要做，保持图上原样
                - 确实看不清就填 null，不要猜
                - reason 用一句中文说明依据（例如"参考区间 316-354，34 少一位"）

                待核对的行：
                %s
                只输出 JSON，不要 Markdown：
                {"items":[{"key":"id:38","value":340,"reason":"..."}]}
                """.formatted(rows);
    }

    /** 解析仲裁结果；无法解析的条目直接忽略，交由用户判断 */
    public static Map<String, Arbitration> parseArbitration(String raw, ObjectMapper mapper) {
        Map<String, Arbitration> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;

        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```$", "").trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) return result;

        try {
            JsonNode root = mapper.readTree(s.substring(start, end + 1));
            for (JsonNode item : root.path("items")) {
                String key = item.path("key").asText(null);
                if (key == null) continue;
                BigDecimal value = null;
                JsonNode v = item.get("value");
                if (v != null && v.isNumber()) {
                    value = v.decimalValue();
                } else if (v != null && v.isTextual() && !v.asText().isBlank()) {
                    try {
                        value = new BigDecimal(v.asText().trim());
                    } catch (NumberFormatException ignored) {
                        // 文本型结果无法参与数值仲裁，保持 null
                    }
                }
                result.put(key, new Arbitration(value, item.path("reason").asText(null)));
            }
        } catch (Exception ignored) {
            // 仲裁失败不影响主流程，两次数值都留给用户
        }
        return result;
    }
}
