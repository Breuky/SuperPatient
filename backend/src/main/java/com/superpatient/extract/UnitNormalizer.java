package com.superpatient.extract;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 单位归一：先统一书写（×10^9/L、10*9/L 视为同一单位），再做少量确定性的换算。
 * 换算表只放含义无歧义的几组，拿不准的一律不换算，交给用户校对。
 */
public final class UnitNormalizer {

    private UnitNormalizer() {
    }

    public record Result(String unit, BigDecimal factor, String warning) {
        public boolean converted() {
            return factor != null && factor.compareTo(BigDecimal.ONE) != 0;
        }
    }

    private static final Pattern POWER_9_OR_12 = Pattern.compile("(?i)^[X×]?10[\\^\\*E]?(9|12)/L$");

    /** key 为 "原始单位>目标单位"，均用大写规范形式 */
    private static final Map<String, BigDecimal> CONVERSIONS = Map.of(
            "G/DL>G/L", BigDecimal.TEN,
            "MG/DL>MG/L", BigDecimal.TEN,
            "MMOL/L>UMOL/L", new BigDecimal("1000"),
            "UMOL/L>MMOL/L", new BigDecimal("0.001")
    );

    public static Result normalize(String rawUnit, String targetUnit) {
        if (targetUnit == null || targetUnit.isBlank()) {
            return new Result(rawUnit, BigDecimal.ONE, null);
        }
        if (rawUnit == null || rawUnit.isBlank()) {
            return new Result(targetUnit, BigDecimal.ONE, null);
        }

        String from = canonical(rawUnit);
        String to = canonical(targetUnit);
        if (from.equals(to)) {
            return new Result(targetUnit, BigDecimal.ONE, null);
        }

        BigDecimal factor = CONVERSIONS.get(from + ">" + to);
        if (factor != null) {
            return new Result(targetUnit, factor, "单位已换算: " + rawUnit.trim() + " → " + targetUnit);
        }
        return new Result(rawUnit.trim(), null, "单位与指标库不一致且无法自动换算: " + rawUnit.trim() + " / " + targetUnit);
    }

    public static String canonical(String unit) {
        if (unit == null) return "";
        String s = unit.replace("\u3000", "")
                .replaceAll("\\s+", "")
                .replace('×', 'X').replace('＊', '*').replace('μ', 'U').replace('µ', 'U')
                .toUpperCase();
        var m = POWER_9_OR_12.matcher(s);
        if (m.matches()) {
            return "10^" + m.group(1) + "/L";
        }
        if (s.equals("U/L") || s.equals("IU/L")) {
            return "U/L";
        }
        return s;
    }
}
