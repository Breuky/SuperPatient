package com.superpatient.service;

import com.superpatient.dto.InsightRequest;
import com.superpatient.dto.InsightVO;
import com.superpatient.dto.RecordDetailVO;
import com.superpatient.dto.ValueVO;
import com.superpatient.mapper.ValueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Agent 服务不可用时必须退回规则摘要，不能让「解读」变成空白
 */
class InsightServiceFallbackTest {

    private RecordService recordService;
    private InsightService service;

    @BeforeEach
    void setUp() {
        recordService = mock(RecordService.class);
        service = new InsightService(recordService, mock(ValueMapper.class),
                mock(com.superpatient.mapper.InsightLogMapper.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                false, "http://127.0.0.1:8001", "qwen-plus", "", "", "", 8080);
    }

    @Test
    void fallsBackToRuleSummaryWhenAgentDisabled() {
        when(recordService.detail(any())).thenReturn(record());

        InsightVO vo = service.explain(request());

        assertEquals("rule", vo.getMode());
        assertTrue(vo.isDegraded());
        assertEquals(2, vo.getHighlights().size());
        assertTrue(vo.getSummary().contains("2 项"));
        assertNotNull(vo.getDisclaimer(), "免责声明必须由服务端给出");
        assertTrue(vo.getDisclaimer().contains("不构成医疗建议"));
    }

    @Test
    void globalChatWithoutRecordReturnsClearFallback() {
        InsightRequest request = new InsightRequest();
        request.setRecordId(null);
        request.setQuestion("我一共查过几次检查？");

        InsightVO vo = service.explain(request);

        assertTrue(vo.isDegraded());
        assertEquals("rule", vo.getMode());
        assertTrue(vo.getSummary().contains("AI 服务暂时不可用"), "没有当前记录时也要给出明确说明");
        assertTrue(vo.getHighlights().isEmpty());
    }

    @Test
    void fallbackDescribesDirectionOfAbnormalValues() {
        when(recordService.detail(any())).thenReturn(record());

        InsightVO vo = service.explain(request());

        InsightVO.Highlight high = vo.getHighlights().get(0);
        assertEquals("空腹血糖", high.getIndicator());
        assertTrue(high.getText().contains("高于"));
        assertEquals("本次报告", high.getBasis());
    }

    private InsightRequest request() {
        InsightRequest r = new InsightRequest();
        r.setRecordId(1L);
        r.setMode("agent");
        return r;
    }

    private RecordDetailVO record() {
        RecordDetailVO vo = new RecordDetailVO();
        vo.setId(1L);
        vo.setExamDate("2026-03-28");
        vo.setCategoryName("抽血");
        vo.setValues(List.of(
                value("空腹血糖", "6.4", 1, "6.1"),
                value("总胆固醇", "5.6", 1, "5.2"),
                value("血红蛋白", "131", 0, "175")));
        return vo;
    }

    private ValueVO value(String name, String v, int abnormal, String max) {
        ValueVO vo = new ValueVO();
        vo.setIndicatorId(1);
        vo.setIndicatorName(name);
        vo.setUnit("mmol/L");
        vo.setValueDecimal(new BigDecimal(v));
        vo.setRefMax(new BigDecimal(max));
        vo.setIsAbnormal(abnormal);
        return vo;
    }
}
