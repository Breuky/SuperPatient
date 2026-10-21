package com.superpatient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superpatient.dto.ExtractResultVO;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.exception.BusinessException;
import com.superpatient.extract.IndicatorMatcher;
import com.superpatient.llm.LlmVisionClient;
import com.superpatient.mapper.IndicatorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExtractionServiceTest {

    private static final String MODEL_OUTPUT = """
            {"examDate":"2026-03-28","hospital":"市第一人民医院","items":[
              {"name":"HGB","value":"12.5","unit":"g/dL","refText":"12.0-16.0"},
              {"name":"白细胞计数","value":"3.2","unit":"10^9/L","refText":"4.0-10.0"},
              {"name":"葡萄糖","value":"5.2","unit":"mmol/L","refText":"3.9-6.1"},
              {"name":"嗜酸性粒细胞百分比","value":"2.1","unit":"%","refText":"0.4-8.0"}
            ]}
            """;

    @Mock private FileService fileService;
    @Mock private IndicatorMapper indicatorMapper;
    @Mock private LlmVisionClient llm;

    private ReportExtractionService service;

    @BeforeEach
    void setUp() {
        service = new ReportExtractionService(fileService, indicatorMapper,
                new IndicatorMatcher(), llm, new ObjectMapper());
    }

    @Test
    void turnsModelOutputIntoReviewableCandidates() throws Exception {
        stubImageAndPool();
        when(llm.extract(any(), any(), any())).thenReturn(MODEL_OUTPUT);
        when(llm.displayName()).thenReturn("mock");

        ExtractResultVO result = service.parse("/api/files/1", 1);

        assertEquals("2026-03-28", result.getExamDate());
        assertEquals("市第一人民医院", result.getHospital());
        assertEquals("mock", result.getEngine());
        assertEquals(3, result.getCandidates().size());
        assertEquals(1, result.getUnmatched().size());
        assertEquals("嗜酸性粒细胞百分比", result.getUnmatched().get(0).getRawName());

        ExtractResultVO.Candidate hgb = result.getCandidates().get(0);
        assertEquals(1, hgb.getIndicatorId());
        assertEquals("alias", hgb.getMatchType());
        assertEquals(0, new BigDecimal("125").compareTo(hgb.getValueDecimal()), "g/dL 应换算为 g/L");
        assertEquals(0, new BigDecimal("120").compareTo(hgb.getRefMin()), "参考区间要跟着一起换算");
        assertEquals(0, new BigDecimal("160").compareTo(hgb.getRefMax()));
        assertEquals(0, hgb.getIsAbnormal());
        assertNotNull(hgb.getWarning());
        assertEquals("HGB", hgb.getRawName());
        assertEquals("g/L", hgb.getUnit());

        ExtractResultVO.Candidate wbc = result.getCandidates().get(1);
        assertEquals(2, wbc.getIsAbnormal(), "3.2 低于 4.0 应为偏低");

        ExtractResultVO.Candidate glu = result.getCandidates().get(2);
        assertEquals(0, new BigDecimal("3.9").compareTo(glu.getRefMin()), "refText 应被解析成上下限");
        assertEquals(0, new BigDecimal("6.1").compareTo(glu.getRefMax()));
    }

    @Test
    void fallsBackToIndicatorDefaultRangeWhenReportHasNone() throws Exception {
        stubImageAndPool();
        when(llm.extract(any(), any(), any())).thenReturn("""
                {"items":[{"name":"白细胞计数","value":"11.0","unit":"10^9/L"}]}
                """);
        when(llm.displayName()).thenReturn("mock");

        ExtractResultVO.Candidate c = service.parse("/api/files/1", 1).getCandidates().get(0);

        assertEquals(0, new BigDecimal("4").compareTo(c.getRefMin()));
        assertEquals(1, c.getIsAbnormal());
    }

    @Test
    void acceptsJsonWrappedInCodeFence() throws Exception {
        stubImageAndPool();
        when(llm.extract(any(), any(), any())).thenReturn("```json\n{\"items\":[]}\n```");
        when(llm.displayName()).thenReturn("mock");

        assertTrue(service.parse("/api/files/1", 1).getCandidates().isEmpty());
    }

    @Test
    void rejectsResponseWithoutItems() throws Exception {
        stubImageAndPool();
        when(llm.extract(any(), any(), any())).thenReturn("{\"error\":\"quota exceeded\"}");
        when(llm.displayName()).thenReturn("mock");

        assertThrows(BusinessException.class, () -> service.parse("/api/files/1", 1));
    }

    @Test
    void rejectsForeignImageUrl() {
        assertThrows(BusinessException.class, () -> service.parse("https://evil.example.com/a.png", 1));
    }

    private void stubImageAndPool() throws Exception {
        when(fileService.load(anyLong()))
                .thenReturn(new FileService.FileContent(new ByteArrayResource(new byte[]{1, 2, 3}), "image/png"));
        when(indicatorMapper.findByCategoryId(anyInt())).thenReturn(List.of(
                indicator(1, "血红蛋白", "HGB,Hb", "g/L", "120", "160"),
                indicator(2, "白细胞计数", "WBC", "10^9/L", "4", "10"),
                indicator(3, "葡萄糖", "GLU", "mmol/L", "3.9", "6.1")
        ));
    }

    private ExamIndicator indicator(int id, String name, String aliases, String unit, String min, String max) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setId(id);
        indicator.setName(name);
        indicator.setAliases(aliases);
        indicator.setUnit(unit);
        indicator.setValueType("number");
        indicator.setNormalMin(new BigDecimal(min));
        indicator.setNormalMax(new BigDecimal(max));
        return indicator;
    }
}
