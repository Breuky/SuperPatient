package com.superpatient.extract;

import com.superpatient.entity.ExamIndicator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 把报告上的指标名匹配到指标库。
 *
 * 只做两种匹配：规范名精确、别名精确。
 *
 * 刻意不做"包含匹配"：{@code 平均红细胞血红蛋白} 含 {@code 血红蛋白}，
 * 但它是完全不同的指标（MCH），按包含规则会被错归到血红蛋白，
 * 把一条血红蛋白变成三条。宁可落进 unmatched 让人工确认，也不要错配。
 */
@Component
public class IndicatorMatcher {

    public record Match(ExamIndicator indicator, String matchType, double confidence) {
    }

    public Match match(String rawName, List<ExamIndicator> pool) {
        String raw = normalize(rawName);
        if (raw.isEmpty() || pool == null || pool.isEmpty()) {
            return null;
        }

        for (ExamIndicator indicator : pool) {
            if (raw.equals(normalize(indicator.getName()))) {
                return new Match(indicator, "exact", 1.0);
            }
        }
        for (ExamIndicator indicator : pool) {
            if (aliasesOf(indicator).contains(raw)) {
                return new Match(indicator, "alias", 0.95);
            }
        }
        return null;
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

    /**
     * 去空格、全角转半角、统一大写，并去掉括号内容：
     * {@code 血小板计数(PLT)} → {@code 血小板计数}，{@code 血红蛋白[g/L]} → {@code 血红蛋白}。
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.replace("\u3000", "")
                .replaceAll("\\s+", "")
                .replace('（', '(').replace('）', ')')
                .replace('％', '%').replace('：', ':')
                .toUpperCase();
        t = t.replaceAll("\\([^)]*\\)", "");
        t = t.replaceAll("\\[[^]]*]", "");
        // 连字符不影响语义：C-反应蛋白 = C反应蛋白，25-OH-VD = 25OHVD
        t = t.replaceAll("[-－—–_]", "");
        // 报告里常用的异常标记与结尾标点
        t = t.replaceAll("^[*＊↑↓▲△☆#]+", "");
        t = t.replaceAll("[*＊↑↓▲△☆.,，。;；]+$", "");
        return t;
    }
}
