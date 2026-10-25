package com.superpatient.extract;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 数值合理性校验：抽取结果的最后一道确定性防线。
 *
 * 只做两类判断，都是「高置信、低误报」的：
 *  1. 量级——值乘/除 10 后恰好落进参考区间，说明八成丢位或多位
 *  2. 基线——与用户本人上次的同指标值相差 8 倍以上
 *
 * 不做「小数位比区间少就报」这种判断：白细胞 4 与区间 3.5-9.5 是常见写法，
 * 按位数判会大面积误报，而误报会让用户直接忽略所有提示。
 *
 * 区间内部的末位截断（20.5 → 20）本质上无法用规则识别，只能靠双读或人工核对。
 */
public final class ValueSanityChecker {

    /** 判定为量级异常所需的倍数 */
    private static final BigDecimal BASELINE_RATIO = new BigDecimal("8");

    private ValueSanityChecker() {
    }

    public record Finding(String code, String message) {
    }

    public static List<Finding> check(BigDecimal value, BigDecimal refMin, BigDecimal refMax,
                                      BigDecimal previous) {
        List<Finding> findings = new ArrayList<>();
        if (value == null) return findings;

        Finding magnitude = checkMagnitude(value, refMin, refMax);
        if (magnitude != null) findings.add(magnitude);

        Finding baseline = checkBaseline(value, previous);
        if (baseline != null) findings.add(baseline);

        return findings;
    }

    /** 值不在区间内，但 ×10 或 ÷10 后在区间内 → 疑似丢位/多位 */
    static Finding checkMagnitude(BigDecimal value, BigDecimal refMin, BigDecimal refMax) {
        if (refMin == null && refMax == null) return null;
        if (inside(value, refMin, refMax)) return null;

        String range = formatRange(refMin, refMax);

        BigDecimal times10 = value.multiply(BigDecimal.TEN);
        if (inside(times10, refMin, refMax)) {
            return new Finding("magnitude",
                    "疑似少一位：" + value.toPlainString() + "，×10 后落在参考区间 " + range);
        }

        BigDecimal divided10 = value.divide(BigDecimal.TEN, 6, RoundingMode.HALF_UP);
        if (inside(divided10, refMin, refMax)) {
            return new Finding("magnitude",
                    "疑似多一位：" + value.toPlainString() + "，÷10 后落在参考区间 " + range);
        }
        return null;
    }

    /** 与本人上次记录相差过大 → 提醒核对，不下结论 */
    static Finding checkBaseline(BigDecimal value, BigDecimal previous) {
        if (value == null || previous == null || previous.signum() == 0) return null;
        if (value.signum() == 0) return null;

        BigDecimal ratio = value.abs()
                .divide(previous.abs(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BASELINE_RATIO) >= 0) {
            return new Finding("baseline",
                    "是上次（" + trimZeros(previous) + "）的 " + ratio.stripTrailingZeros().toPlainString() + " 倍，请核对");
        }
        BigDecimal inverse = BigDecimal.ONE.divide(ratio, 4, RoundingMode.HALF_UP);
        if (inverse.compareTo(BASELINE_RATIO) >= 0) {
            return new Finding("baseline",
                    "只有上次（" + trimZeros(previous) + "）的 1/" + inverse.stripTrailingZeros().toPlainString() + "，请核对");
        }
        return null;
    }

    private static boolean inside(BigDecimal v, BigDecimal min, BigDecimal max) {
        if (min != null && v.compareTo(min) < 0) return false;
        if (max != null && v.compareTo(max) > 0) return false;
        return true;
    }

    private static String formatRange(BigDecimal min, BigDecimal max) {
        return (min == null ? "-" : trimZeros(min)) + "~" + (max == null ? "-" : trimZeros(max));
    }

    private static String trimZeros(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
