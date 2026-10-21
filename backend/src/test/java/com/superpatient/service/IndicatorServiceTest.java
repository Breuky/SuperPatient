package com.superpatient.service;

import com.superpatient.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorServiceTest {

    @Test
    void normalizesSeparatorsAndSpaces() {
        assertEquals("HGB,Hb,HB", IndicatorService.normalizeAliases("HGB, Hb ,  HB "));
    }

    @Test
    void acceptsFullWidthSeparators() {
        assertEquals("HGB,Hb,HB", IndicatorService.normalizeAliases("HGB，Hb、HB"));
    }

    @Test
    void dropsDuplicatesAndBlanks() {
        assertEquals("WBC,白细胞", IndicatorService.normalizeAliases("WBC,,白细胞,WBC, "));
    }

    @Test
    void keepsNullAndEmptyDistinct() {
        assertNull(IndicatorService.normalizeAliases(null), "null 表示不修改");
        assertEquals("", IndicatorService.normalizeAliases(""), "空字符串表示清空");
        assertEquals("", IndicatorService.normalizeAliases("  ,  "));
    }

    @Test
    void rejectsOverlongAliases() {
        assertThrows(BusinessException.class,
                () -> IndicatorService.normalizeAliases("A".repeat(300)));
    }
}
