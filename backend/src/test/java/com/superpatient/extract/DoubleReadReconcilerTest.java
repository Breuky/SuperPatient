package com.superpatient.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.dto.ExtractResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双读比对：一致直接采用、不一致仲裁、仲裁失败保留两次数值
 */
class DoubleReadReconcilerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void identicalReadsProduceNoConflict() {
        ExtractResultVO a = vo(candidate(38, "平均红细胞血红蛋白浓度", "340", "g/L"));
        ExtractResultVO b = vo(candidate(38, "平均红细胞血红蛋白浓度", "340", "g/L"));

        assertTrue(DoubleReadReconciler.findConflicts(a, b).isEmpty());

        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of());
        assertEquals(1, merged.getCandidates().size());
        assertNull(merged.getCandidates().get(0).getConflict());
    }

    @Test
    void treatsTrailingZerosAsSameRead() {
        ExtractResultVO a = vo(candidate(1, "血红蛋白", "130.0", "g/L"));
        ExtractResultVO b = vo(candidate(1, "血红蛋白", "130", "g/L"));

        assertTrue(DoubleReadReconciler.findConflicts(a, b).isEmpty(),
                "130 与 130.0 是同一个读数");
    }

    @Test
    void detectsDigitLossAndKeepsBothValuesWhenNoArbitration() {
        ExtractResultVO a = vo(candidate(38, "平均红细胞血红蛋白浓度", "340", "g/L"));
        ExtractResultVO b = vo(candidate(38, "平均红细胞血红蛋白浓度", "34", "g/L"));

        List<DoubleReadReconciler.ConflictRow> conflicts = DoubleReadReconciler.findConflicts(a, b);
        assertEquals(1, conflicts.size());

        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of());
        ExtractResultVO.Candidate c = merged.getCandidates().get(0);
        assertEquals("340", c.getValueDecimal().toPlainString(), "无仲裁时保留第一次读到的值");
        assertEquals("340", c.getConflict().getFirst().toPlainString());
        assertEquals("34", c.getConflict().getSecond().toPlainString());
        assertNull(c.getConflict().getResolved());
        assertTrue(c.getWarning().contains("两次识别不一致"));
    }

    @Test
    void arbitrationOverridesValueAndExplainsWhy() {
        ExtractResultVO a = vo(candidate(38, "平均红细胞血红蛋白浓度", "340", "g/L"));
        ExtractResultVO b = vo(candidate(38, "平均红细胞血红蛋白浓度", "34", "g/L"));

        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of(
                "id:38", new DoubleReadReconciler.Arbitration(
                        new BigDecimal("340"), "参考区间 316-354，34 少一位")));

        ExtractResultVO.Candidate c = merged.getCandidates().get(0);
        assertEquals("340", c.getValueDecimal().toPlainString());
        assertEquals("340", c.getConflict().getResolved().toPlainString());
        assertTrue(c.getWarning().contains("少一位"));
    }

    @Test
    void flagsRowMissingInOneRead() {
        ExtractResultVO a = vo(candidate(45, "嗜酸性粒细胞绝对值", "0.1", "10^9/L"));
        ExtractResultVO b = vo();

        List<DoubleReadReconciler.ConflictRow> conflicts = DoubleReadReconciler.findConflicts(a, b);
        assertEquals(1, conflicts.size());

        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of());
        ExtractResultVO.Candidate c = merged.getCandidates().get(0);
        assertTrue(c.getConflict().isMissingInOneRead());
        assertEquals("0.1", c.getValueDecimal().toPlainString(), "只读到一次时仍保留该行");
    }

    @Test
    void detectsTextValueMismatch() {
        ExtractResultVO.Candidate x = candidate(20, "尿蛋白", null, null);
        x.setValueText("阴性");
        ExtractResultVO.Candidate y = candidate(20, "尿蛋白", null, null);
        y.setValueText("+");

        assertEquals(1, DoubleReadReconciler.findConflicts(vo(x), vo(y)).size());
    }

    @Test
    void unmatchedRowValuesAreAlsoCompared() {
        ExtractResultVO a = new ExtractResultVO();
        a.setCandidates(List.of());
        a.setUnmatched(List.of(unmatched("大血小板比率", "20.5")));
        ExtractResultVO b = new ExtractResultVO();
        b.setCandidates(List.of());
        b.setUnmatched(List.of(unmatched("大血小板比率", "20")));

        List<DoubleReadReconciler.ConflictRow> conflicts = DoubleReadReconciler.findConflicts(a, b);
        assertEquals(1, conflicts.size(), "未匹配项两次读数不同也应进仲裁");
        assertTrue(conflicts.get(0).key().startsWith("raw:"));

        String key = conflicts.get(0).key();
        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of(
                key, new DoubleReadReconciler.Arbitration(new BigDecimal("20.5"), "小数点被截断")));
        assertEquals("20.5", merged.getUnmatched().get(0).getRawValue(), "仲裁值应覆盖原文值");
    }

    @Test
    void unmatchedRowsAreMergedWithoutDuplicates() {
        ExtractResultVO a = new ExtractResultVO();
        a.setCandidates(List.of());
        a.setUnmatched(List.of(unmatched("大血小板比率", "20.5")));
        ExtractResultVO b = new ExtractResultVO();
        b.setCandidates(List.of());
        b.setUnmatched(List.of(unmatched("大血小板比率", "20.5")));

        ExtractResultVO merged = DoubleReadReconciler.merge(a, b, Map.of());
        assertEquals(1, merged.getUnmatched().size(), "未匹配项去重后只保留一行");
    }

    @Test
    void parsesArbitrationWrappedInCodeFence() {
        String raw = "```json\n{\"items\":[{\"key\":\"id:38\",\"value\":340,\"reason\":\"区间为 316-354\"}]}\n```";

        Map<String, DoubleReadReconciler.Arbitration> parsed =
                DoubleReadReconciler.parseArbitration(raw, mapper);

        assertEquals(1, parsed.size());
        assertEquals(340, parsed.get("id:38").value().intValue());
        assertTrue(parsed.get("id:38").reason().contains("316-354"));
    }

    @Test
    void ignoresUnparsableArbitrationInsteadOfFailing() {
        assertTrue(DoubleReadReconciler.parseArbitration("模型今天不想说话", mapper).isEmpty());
        assertTrue(DoubleReadReconciler.parseArbitration(null, mapper).isEmpty());

        String nullValue = "{\"items\":[{\"key\":\"id:38\",\"value\":null,\"reason\":\"看不清\"}]}";
        Map<String, DoubleReadReconciler.Arbitration> parsed =
                DoubleReadReconciler.parseArbitration(nullValue, mapper);
        assertNull(parsed.get("id:38").value(), "看不清时不给值，交给用户判断");
    }

    @Test
    void arbitrationPromptCarriesBothReadsAndRange() {
        ExtractResultVO a = vo(candidate(38, "平均红细胞血红蛋白浓度", "340", "g/L"));
        ExtractResultVO b = vo(candidate(38, "平均红细胞血红蛋白浓度", "34", "g/L"));
        a.getCandidates().get(0).setRefText("参考区间：316-354 g/L");

        String prompt = DoubleReadReconciler.buildArbitrationPrompt(
                DoubleReadReconciler.findConflicts(a, b));

        assertTrue(prompt.contains("340"));
        assertTrue(prompt.contains("34"));
        assertTrue(prompt.contains("316-354"));
        assertTrue(prompt.contains("id:38"));
    }

    private ExtractResultVO vo(ExtractResultVO.Candidate... candidates) {
        ExtractResultVO vo = new ExtractResultVO();
        vo.setCategoryId(1);
        vo.setEngine("test");
        vo.setCandidates(List.of(candidates));
        vo.setUnmatched(List.of());
        return vo;
    }

    private ExtractResultVO.Candidate candidate(int id, String name, String value, String unit) {
        ExtractResultVO.Candidate c = new ExtractResultVO.Candidate();
        c.setIndicatorId(id);
        c.setIndicatorName(name);
        c.setRawName(name);
        c.setUnit(unit);
        c.setRawUnit(unit);
        if (value != null) c.setValueDecimal(new BigDecimal(value));
        c.setConfidence(1.0);
        c.setMatchType("exact");
        return c;
    }

    private ExtractResultVO.Unmatched unmatched(String name, String value) {
        ExtractResultVO.Unmatched u = new ExtractResultVO.Unmatched();
        u.setRawName(name);
        u.setRawValue(value);
        return u;
    }
}
