package com.superpatient.service;

import com.superpatient.dto.OverviewVO;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.util.CurrentHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** 概览统计只做组装，范围固定为当前用户 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private RecordMapper recordMapper;
    @InjectMocks private StatisticsService service;

    @BeforeEach
    void setUp() {
        CurrentHolder.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentHolder.remove();
    }

    @Test
    void assemblesBaseCategoriesAndTopAbnormal() {
        OverviewVO base = new OverviewVO();
        base.setTotalRecords(3);
        base.setFirstDate(LocalDate.of(2026, 3, 28));
        base.setLastDate(LocalDate.of(2026, 6, 15));
        base.setTotalValues(12);
        base.setAbnormalValues(2);

        OverviewVO.CategoryCount cat = new OverviewVO.CategoryCount();
        cat.setName("抽血");
        cat.setCount(2);
        OverviewVO.IndicatorCount ind = new OverviewVO.IndicatorCount();
        ind.setName("空腹血糖");
        ind.setTimes(1);

        when(recordMapper.overviewBase(1L)).thenReturn(base);
        when(recordMapper.countByCategory(1L)).thenReturn(List.of(cat));
        when(recordMapper.topAbnormal(1L, 5)).thenReturn(List.of(ind));

        OverviewVO vo = service.overview();

        assertEquals(3, vo.getTotalRecords());
        assertEquals(LocalDate.of(2026, 3, 28), vo.getFirstDate());
        assertEquals(1, vo.getCategories().size());
        assertEquals("空腹血糖", vo.getTopAbnormal().get(0).getName());
    }

    @Test
    void toleratesUserWithoutAnyRecord() {
        when(recordMapper.overviewBase(anyLong())).thenReturn(null);
        when(recordMapper.countByCategory(anyLong())).thenReturn(List.of());
        when(recordMapper.topAbnormal(anyLong(), anyInt())).thenReturn(List.of());

        OverviewVO vo = service.overview();

        assertEquals(0, vo.getTotalRecords(), "没有记录时返回 0 而不是 null");
        assertTrue(vo.getCategories().isEmpty());
        assertTrue(vo.getTopAbnormal().isEmpty());
    }
}
