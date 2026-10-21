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
            indicator(5, "总胆红素", "TBIL")
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
    void matchesLongerReportNameByContains() {
        IndicatorMatcher.Match m = matcher.match("血小板计数(PLT)", pool);
        assertEquals(3, m.indicator().getId());
    }

    @Test
    void doesNotConfuseSiblingIndicators() {
        assertEquals(4, matcher.match("直接胆红素", pool).indicator().getId());
        assertEquals(5, matcher.match("总胆红素", pool).indicator().getId());
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
