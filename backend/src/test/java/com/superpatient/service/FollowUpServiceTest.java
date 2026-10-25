package com.superpatient.service;

import com.superpatient.dto.FollowUpSource;
import com.superpatient.entity.FollowUp;
import com.superpatient.mapper.FollowUpMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 随访提醒规则：异常才提醒、有历史才算间隔、到期才生成、连续异常降半
 */
@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock private FollowUpMapper followUpMapper;

    @InjectMocks private FollowUpService followUpService;

    private static final long USER = 1L;

    @Test
    void noReminderWhenLatestIsNormal() {
        LocalDate today = LocalDate.now();
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, today.minusDays(200), 0),
                row(1, today.minusDays(400), 1)));

        followUpService.refresh(USER);

        verify(followUpMapper, never()).upsert(any());
        verify(followUpMapper).closeAllPending(USER, 1);
    }

    @Test
    void noReminderWithSingleRecord() {
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, LocalDate.now().minusDays(300), 1)));

        followUpService.refresh(USER);

        verify(followUpMapper, never()).upsert(any());
    }

    @Test
    void noReminderBeforeDueDate() {
        LocalDate today = LocalDate.now();
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, today.minusDays(10), 1),
                row(1, today.minusDays(110), 0)));

        followUpService.refresh(USER);

        verify(followUpMapper, never()).upsert(any());
    }

    @Test
    void createsReminderWhenDueUsingMedianInterval() {
        LocalDate today = LocalDate.now();
        LocalDate last = today.minusDays(100);
        // 间隔 90、100、110 → 中位数 100
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, last, 1),
                row(1, last.minusDays(90), 0),
                row(1, last.minusDays(190), 0),
                row(1, last.minusDays(300), 0)));

        followUpService.refresh(USER);

        FollowUp saved = captureUpsert();
        assertEquals(100, saved.getIntervalDays());
        assertEquals(last.plusDays(100), saved.getDueDate());
        assertEquals(0, saved.getPriority());
        assertTrue(saved.getReason().contains("偏高"));
        assertTrue(saved.getReason().contains("100 天"));
    }

    @Test
    void consecutiveAbnormalHalvesInterval() {
        LocalDate today = LocalDate.now();
        LocalDate last = today.minusDays(60);
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, last, 1),
                row(1, last.minusDays(100), 1),
                row(1, last.minusDays(200), 0)));

        followUpService.refresh(USER);

        FollowUp saved = captureUpsert();
        assertEquals(50, saved.getIntervalDays(), "连续异常时中位间隔减半");
        assertEquals(1, saved.getPriority());
        assertTrue(saved.getReason().contains("连续 2 次"));
    }

    @Test
    void closesStaleReminderWhenNewerExamExists() {
        LocalDate last = LocalDate.now().minusDays(10);
        when(followUpMapper.findHistory(USER)).thenReturn(List.of(
                row(1, last, 1),
                row(1, last.minusDays(100), 1)));

        followUpService.refresh(USER);

        verify(followUpMapper).closeStale(USER, 1, last);
    }

    @Test
    void medianIgnoresOutliersByTakingMiddleValue() {
        List<FollowUpSource> history = List.of(
                row(1, LocalDate.of(2026, 3, 1), 1),
                row(1, LocalDate.of(2025, 11, 1), 1),   // 120 天
                row(1, LocalDate.of(2025, 10, 20), 1)); // 12 天

        assertEquals(66, FollowUpService.medianIntervalDays(history));
    }

    @Test
    void statusUpdateRejectsUnknownStatus() {
        assertFalse(followUpService.updateStatus(1L, "deleted", USER));
        verify(followUpMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void statusUpdateOnlyTouchesOwnRows() {
        when(followUpMapper.updateStatus(9L, USER, "dismissed")).thenReturn(1);

        assertTrue(followUpService.updateStatus(9L, "dismissed", USER));
        verify(followUpMapper).updateStatus(9L, USER, "dismissed");
    }

    private FollowUp captureUpsert() {
        ArgumentCaptor<FollowUp> captor = ArgumentCaptor.forClass(FollowUp.class);
        verify(followUpMapper).upsert(captor.capture());
        return captor.getValue();
    }

    private FollowUpSource row(int indicatorId, LocalDate date, int abnormal) {
        FollowUpSource r = new FollowUpSource();
        r.setIndicatorId(indicatorId);
        r.setIndicatorName("指标" + indicatorId);
        r.setUnit("mmol/L");
        r.setExamDate(date);
        r.setIsAbnormal(abnormal);
        return r;
    }
}
