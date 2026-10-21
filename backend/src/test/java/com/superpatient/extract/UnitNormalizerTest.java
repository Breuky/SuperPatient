package com.superpatient.extract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnitNormalizerTest {

    @Test
    void treatsEquivalentNotationsAsSameUnit() {
        for (String variant : new String[]{"×10^9/L", "10*9/L", "10^9/l", "X10^9/L"}) {
            UnitNormalizer.Result r = UnitNormalizer.normalize(variant, "10^9/L");
            assertFalse(r.converted(), variant + " 应当被视为同单位");
            assertNull(r.warning(), variant);
        }
    }

    @Test
    void convertsKnownPairs() {
        UnitNormalizer.Result r = UnitNormalizer.normalize("g/dL", "g/L");
        assertTrue(r.converted());
        assertEquals(0, new BigDecimal("125").compareTo(new BigDecimal("12.5").multiply(r.factor())));
        assertEquals("g/L", r.unit());
    }

    @Test
    void convertsMicroUnits() {
        UnitNormalizer.Result r = UnitNormalizer.normalize("μmol/L", "mmol/L");
        assertTrue(r.converted());
        assertEquals(0, new BigDecimal("0.001").compareTo(r.factor()));
    }

    @Test
    void refusesUnknownConversionInsteadOfGuessing() {
        UnitNormalizer.Result r = UnitNormalizer.normalize("mg/dL", "10^9/L");
        assertNull(r.factor());
        assertEquals("mg/dL", r.unit());
        assertNotNull(r.warning());
    }

    @Test
    void keepsTargetUnitWhenRawUnitMissing() {
        UnitNormalizer.Result r = UnitNormalizer.normalize(null, "g/L");
        assertEquals("g/L", r.unit());
        assertFalse(r.converted());
    }
}
