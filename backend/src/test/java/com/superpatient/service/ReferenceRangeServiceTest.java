package com.superpatient.service;

import com.superpatient.entity.ReferenceRange;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则匹配：性别/年龄都要命中，再按优先级与具体程度排序。
 */
class ReferenceRangeServiceTest {

    private final ReferenceRangeService.PatientContext maleAdult =
            new ReferenceRangeService.PatientContext(1, 30);
    private final ReferenceRangeService.PatientContext femaleAdult =
            new ReferenceRangeService.PatientContext(0, 30);
    private final ReferenceRangeService.PatientContext boy =
            new ReferenceRangeService.PatientContext(1, 10);

    private final List<ReferenceRange> hemoglobin = List.of(
            rule(1, 130, 175, 10, 18, null),
            rule(0, 115, 150, 10, 18, null),
            rule(null, 110, 160, 10, 0, 17)
    );

    @Test
    void picksRangeByGenderForAdults() {
        assertEquals(0, new BigDecimal("130").compareTo(ReferenceRangeService.match(hemoglobin, maleAdult).min()));
        assertEquals(0, new BigDecimal("115").compareTo(ReferenceRangeService.match(hemoglobin, femaleAdult).min()));
    }

    @Test
    void picksChildRangeWhenAgeIsMinor() {
        ReferenceRangeService.Resolved r = ReferenceRangeService.match(hemoglobin, boy);
        assertEquals(0, new BigDecimal("110").compareTo(r.min()));
        assertEquals(0, new BigDecimal("160").compareTo(r.max()));
    }

    @Test
    void skipsAgeBoundedRulesWhenAgeUnknown() {
        ReferenceRangeService.PatientContext unknownAge =
                new ReferenceRangeService.PatientContext(1, null);
        assertNull(ReferenceRangeService.match(hemoglobin, unknownAge),
                "算不出年龄时不能套用限定年龄的区间，宁可回落默认值");
    }

    @Test
    void prefersHigherPriority() {
        List<ReferenceRange> rules = List.of(
                rule(1, 130, 175, 10, 18, null),
                rule(1, 140, 180, 99, 18, null)
        );
        assertEquals(0, new BigDecimal("140").compareTo(
                ReferenceRangeService.match(rules, maleAdult).min()));
    }

    @Test
    void prefersMoreSpecificRule() {
        List<ReferenceRange> rules = List.of(
                rule(1, 120, 170, 10, null, null),
                rule(1, 130, 175, 10, 18, 60)
        );
        assertEquals(0, new BigDecimal("130").compareTo(
                ReferenceRangeService.match(rules, maleAdult).min()),
                "同样优先级下，同时限定性别和年龄的规则更具体");
    }

    @Test
    void prefersNarrowerAgeRange() {
        List<ReferenceRange> rules = List.of(
                rule(null, 100, 200, 10, 0, 17),
                rule(null, 105, 190, 10, 0, 6)
        );
        ReferenceRangeService.PatientContext kid = new ReferenceRangeService.PatientContext(null, 3);
        assertEquals(0, new BigDecimal("105").compareTo(
                ReferenceRangeService.match(rules, kid).min()));
    }

    @Test
    void returnsNullWhenNothingApplies() {
        assertNull(ReferenceRangeService.match(List.of(), maleAdult));
        assertNull(ReferenceRangeService.match(hemoglobin, new ReferenceRangeService.PatientContext(null, 30)),
                "患者性别未知时，性别专用规则不应命中");
    }

    private ReferenceRange rule(Integer gender, int min, int max, int priority, Integer ageMin, Integer ageMax) {
        ReferenceRange r = new ReferenceRange();
        r.setGender(gender);
        r.setRefMin(new BigDecimal(min));
        r.setRefMax(new BigDecimal(max));
        r.setPriority(priority);
        r.setAgeMin(ageMin);
        r.setAgeMax(ageMax);
        return r;
    }
}
