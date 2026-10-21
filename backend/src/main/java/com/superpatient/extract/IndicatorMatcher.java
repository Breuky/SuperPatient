package com.superpatient.extract;

import com.superpatient.entity.ExamIndicator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 把报告上的指标名匹配到指标库。
 * 顺序：规范名精确 → 别名精确 → 双向包含（取最长匹配）。
 * 匹配不到时返回 null，交由用户在校对页手动指定或直接丢弃。
 */
@Component
public class IndicatorMatcher {

    public record Match(ExamIndicator indicator, String matchType, double confidence) {
    }

    public Match match(String rawName, List<ExamIndicator> pool) {
        if (rawName == null || rawName.isBlank() || pool == null || pool.isEmpty()) {
            return null;
        }
        String raw = normalize(rawName);
        if (raw.isEmpty()) {
            return null;
        }

        Match best = null;
        for (ExamIndicator indicator : pool) {
            double score = score(raw, indicator);
            if (score > 0 && (best == null || score > best.confidence())) {
                best = new Match(indicator, typeOf(score), score);
            }
        }
        return best;
    }

    private double score(String raw, ExamIndicator indicator) {
        String name = normalize(indicator.getName());
        if (!name.isEmpty() && raw.equals(name)) {
            return 1.0;
        }
        for (String alias : aliasesOf(indicator)) {
            if (alias.equals(raw)) {
                return 0.95;
            }
        }
        if (raw.length() >= 2 && name.length() >= 2) {
            if (raw.contains(name)) {
                return Math.max(0.55, 0.85 - 0.05 * (raw.length() - name.length()));
            }
            if (name.contains(raw)) {
                return Math.max(0.55, 0.75 - 0.05 * (name.length() - raw.length()));
            }
        }
        return 0;
    }

    private String typeOf(double score) {
        if (score >= 0.99) return "exact";
        if (score >= 0.9) return "alias";
        return "contains";
    }

    private List<String> aliasesOf(ExamIndicator indicator) {
        if (indicator.getAliases() == null || indicator.getAliases().isBlank()) {
            return List.of();
        }
        return Arrays.stream(indicator.getAliases().split("[,，]"))
                .map(IndicatorMatcher::normalize)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 去空格、全角转半角、统一大写；中文不受影响 */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.replace("\u3000", "")
                .replaceAll("\\s+", "")
                .replace('（', '(').replace('）', ')')
                .replace('％', '%').replace('：', ':')
                .toUpperCase();
    }
}
