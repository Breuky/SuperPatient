package com.superpatient.service;

import com.superpatient.dto.RecordCreateRequest;
import com.superpatient.dto.ValueItem;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.entity.ExamValue;
import com.superpatient.entity.User;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.CategoryMapper;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 校验 isAbnormal 的判定规则：0=正常 1=偏高 2=偏低
 */
@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock private RecordMapper recordMapper;
    @Mock private ValueMapper valueMapper;
    @Mock private UserMapper userMapper;
    @Mock private IndicatorMapper indicatorMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ReferenceRangeService referenceRangeService;

    @InjectMocks private RecordService recordService;

    @BeforeEach
    void setUp() {
        CurrentHolder.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentHolder.remove();
    }

    @Test
    void flagsValuesOutsideIndicatorRange() {
        when(indicatorMapper.findById(1)).thenReturn(indicator(1, "4", "10"));   // 低于下限
        when(indicatorMapper.findById(2)).thenReturn(indicator(2, "4", "10"));   // 区间内
        when(indicatorMapper.findById(3)).thenReturn(indicator(3, "4", "10"));   // 高于上限
        when(indicatorMapper.findById(4)).thenReturn(indicator(4, "4", "10"));   // 恰好等于上限

        recordService.create(request(
                item(1, "3.2"),
                item(2, "6.8"),
                item(3, "12.0"),
                item(4, "10.0")));

        assertEquals(List.of(2, 0, 1, 0), capturedAbnormalFlags());
    }

    @Test
    void treatsIndicatorWithoutRangeAsNormal() {
        when(indicatorMapper.findById(5)).thenReturn(indicator(5, null, null));

        recordService.create(request(item(5, "999")));

        assertEquals(List.of(0), capturedAbnormalFlags());
    }

    @Test
    void reportRangeOverridesIndicatorDefault() {
        when(indicatorMapper.findById(1)).thenReturn(indicator(1, "4", "10"));
        ValueItem item = item(1, "3.2");
        item.setRefMin(new BigDecimal("3"));
        item.setRefMax(new BigDecimal("5"));

        recordService.create(request(item));

        ExamValue saved = capturedValues().get(0);
        assertEquals(0, saved.getIsAbnormal(), "按报告区间 3~5 判断，3.2 属正常");
        assertEquals(0, new BigDecimal("3").compareTo(saved.getRefMin()));
        assertEquals(0, new BigDecimal("5").compareTo(saved.getRefMax()));
        assertEquals("report", saved.getRefSource());
    }

    @Test
    void writesRecordUnderCurrentUser() {
        when(indicatorMapper.findById(1)).thenReturn(indicator(1, "4", "10"));

        recordService.create(request(item(1, "6.8")));

        ArgumentCaptor<com.superpatient.entity.ExamRecord> captor =
                ArgumentCaptor.forClass(com.superpatient.entity.ExamRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId(), "记录直接挂在当前登录用户上");
    }

    @Test
    void rejectsValueForAnotherUsersPrivateIndicator() {
        ExamIndicator foreign = indicator(9, "4", "10");
        foreign.setOwnerId(999L);
        when(indicatorMapper.findById(9)).thenReturn(foreign);

        BusinessException e = assertThrows(BusinessException.class,
                () -> recordService.create(request(item(9, "6.8"))));
        assertTrue(e.getMessage().contains("无权"));
    }

    private List<ExamValue> capturedValues() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExamValue>> captor = ArgumentCaptor.forClass(List.class);
        verify(valueMapper).batchInsert(captor.capture());
        return captor.getValue();
    }

    private List<Integer> capturedAbnormalFlags() {
        return capturedValues().stream().map(ExamValue::getIsAbnormal).toList();
    }

    private RecordCreateRequest request(ValueItem... items) {
        RecordCreateRequest request = new RecordCreateRequest();
        request.setCategoryId(1);
        request.setExamDate("2026-01-01T10:00:00");
        request.setValues(List.of(items));
        return request;
    }

    private ValueItem item(int indicatorId, String value) {
        ValueItem item = new ValueItem();
        item.setIndicatorId(indicatorId);
        item.setValueDecimal(new BigDecimal(value));
        return item;
    }

    private ExamIndicator indicator(int id, String min, String max) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setId(id);
        indicator.setName("指标" + id);
        indicator.setValueType("number");
        indicator.setNormalMin(min == null ? null : new BigDecimal(min));
        indicator.setNormalMax(max == null ? null : new BigDecimal(max));
        return indicator;
    }
}
