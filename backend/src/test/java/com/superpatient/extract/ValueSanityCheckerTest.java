package com.superpatient.extract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数值合理性校验：只抓高置信的量级错误与基线突变，宁可漏报也不刷屏
 */
class ValueSanityCheckerTest {

    @Test
    void flagsDigitLossWhenTenTimesLandsInRange() {
        List<ValueSanityChecker.Finding> findings = ValueSanityChecker.check(
                new BigDecimal("34"), new BigDecimal("316"), new BigDecimal("354"), null);

        assertEquals(1, findings.size());
        assertEquals("magnitude", findings.get(0).code());
        assertTrue(findings.get(0).message().contains("少一位"));
    }

    @Test
    void flagsExtraDigitWhenDividedByTenLandsInRange() {
        List<ValueSanityChecker.Finding> findings = ValueSanityChecker.check(
                new BigDecimal("3400"), new BigDecimal("316"), new BigDecimal("354"), null);

        assertEquals(1, findings.size());
        assertTrue(findings.get(0).message().contains("多一位"));
    }

    @Test
    void staysSilentForValuesInsideRange() {
        assertTrue(ValueSanityChecker.check(
                new BigDecimal("340"), new BigDecimal("316"), new BigDecimal("354"), null).isEmpty());

        assertTrue(ValueSanityChecker.check(
                new BigDecimal("3.2"), new BigDecimal("4"), new BigDecimal("10"), null).isEmpty(),
                "真实的偏低值不该被当成错误");
    }

    @Test
    void staysSilentWithoutRange() {
        assertTrue(ValueSanityChecker.check(new BigDecimal("34"), null, null, null).isEmpty());
    }

    @Test
    void flagsOrderOfMagnitudeChangeAgainstOwnHistory() {
        List<ValueSanityChecker.Finding> findings = ValueSanityChecker.check(
                new BigDecimal("130"), new BigDecimal("120"), new BigDecimal("160"), new BigDecimal("13"));

        assertTrue(findings.stream().anyMatch(f -> f.code().equals("baseline")));
        assertTrue(findings.stream().anyMatch(f -> f.message().contains("10 倍")));
    }

    @Test
    void doesNotFlagNormalVariation() {
        assertTrue(ValueSanityChecker.check(
                new BigDecimal("131"), new BigDecimal("120"), new BigDecimal("160"), new BigDecimal("128")).isEmpty(),
                "正常波动（131 vs 128）不该提示");
    }

    @Test
    void ignoresZeroHistory() {
        assertTrue(ValueSanityChecker.check(
                new BigDecimal("5"), null, null, BigDecimal.ZERO).isEmpty());
    }
}
