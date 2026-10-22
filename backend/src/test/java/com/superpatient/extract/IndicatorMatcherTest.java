package com.superpatient.extract;

import com.superpatient.entity.ExamIndicator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IndicatorMatcherTest {

    private final IndicatorMatcher matcher = new IndicatorMatcher();

    private final List<ExamIndicator> pool = List.of(
            indicator(1, "血红蛋白", "HGB,Hb,HB"),
            indicator(2, "白细胞计数", "WBC,白细胞,LEU"),
            indicator(3, "血小板计数", "PLT,血小板"),
            indicator(4, "直接胆红素", "DBIL,结合胆红素"),
            indicator(5, "总胆红素", "TBIL"),
            indicator(6, "C反应蛋白", "CRP,hs-CRP")
    );

    @Test
    void matchesExactName() {
        IndicatorMatcher.Match m = matcher.match("白细胞计数", pool);
        assertEquals(2, m.indicator().getId());
        assertEquals("exact", m.matchType());
    }

    @Test
    void matchesAliasIgnoringCaseAndSpaces() {
        IndicatorMatcher.Match m = matcher.match(" wbc ", pool);
        assertEquals(2, m.indicator().getId());
        assertEquals("alias", m.matchType());
        assertEquals(0.95, m.confidence());
    }

    @Test
    void stripsParentheticalSuffixBeforeMatching() {
        assertEquals(3, matcher.match("血小板计数(PLT)", pool).indicator().getId());
        assertEquals(1, matcher.match("血红蛋白（HGB）", pool).indicator().getId());
    }

    @Test
    void ignoresMarkersAndPunctuation() {
        assertEquals(2, matcher.match("*白细胞计数", pool).indicator().getId());
        assertEquals(2, matcher.match("白细胞计数↑", pool).indicator().getId());
        assertEquals(1, matcher.match("血红蛋白.", pool).indicator().getId());
        assertEquals(6, matcher.match("C-反应蛋白", pool).indicator().getId(), "连字符不影响匹配");
    }

    @Test
    void doesNotConfuseSiblingIndicators() {
        assertEquals(4, matcher.match("直接胆红素", pool).indicator().getId());
        assertEquals(5, matcher.match("总胆红素", pool).indicator().getId());
    }

    /**
     * 回归用例：真实化验单里 "平均红细胞血红蛋白" 和 "平均红细胞血红蛋白浓度"
     * 都含 "血红蛋白"，早期用包含匹配会把它们错归成血红蛋白。
     */
    @Test
    void doesNotMatchCompositeNamesBySubstring() {
        assertNull(matcher.match("平均红细胞血红蛋白", pool), "MCH 不是血红蛋白");
        assertNull(matcher.match("平均红细胞血红蛋白浓度", pool), "MCHC 不是血红蛋白");
        assertNull(matcher.match("中性粒细胞绝对值", pool), "绝对值与百分比是两个指标");
    }

    @Test
    void returnsNullWhenNothingMatches() {
        assertNull(matcher.match("嗜酸性粒细胞百分比", pool));
        assertNull(matcher.match("", pool));
        assertNull(matcher.match(null, pool));
    }

    private ExamIndicator indicator(int id, String name, String aliases) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setId(id);
        indicator.setName(name);
        indicator.setAliases(aliases);
        indicator.setValueType("number");
        return indicator;
    }
}
