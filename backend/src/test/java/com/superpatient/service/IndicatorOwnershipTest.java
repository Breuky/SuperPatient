package com.superpatient.service;

import com.superpatient.dto.IndicatorUpdateRequest;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.IndicatorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指标分公共与私有：别人的私有指标必须表现得像不存在
 */
@ExtendWith(MockitoExtension.class)
class IndicatorOwnershipTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;

    @Mock private IndicatorMapper indicatorMapper;

    @InjectMocks private IndicatorService indicatorService;

    @Test
    void publicIndicatorIsVisibleToEveryone() {
        assertTrue(indicator(0L).isVisibleTo(ME));
        assertTrue(indicator(0L).isVisibleTo(OTHER));
    }

    @Test
    void privateIndicatorIsVisibleOnlyToOwner() {
        assertTrue(indicator(ME).isVisibleTo(ME));
        assertFalse(indicator(ME).isVisibleTo(OTHER), "别人的私有指标不可见");
        assertFalse(indicator(ME).isVisibleTo(null), "未登录不享有私有指标");
    }

    @Test
    void cannotUpdateAnotherUsersPrivateIndicator() {
        when(indicatorMapper.findById(7)).thenReturn(indicator(OTHER));

        assertThrows(BusinessException.class,
                () -> indicatorService.update(7, new IndicatorUpdateRequest(), ME));
        verify(indicatorMapper, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cannotDeleteAnotherUsersPrivateIndicator() {
        when(indicatorMapper.findById(7)).thenReturn(indicator(OTHER));

        assertThrows(BusinessException.class, () -> indicatorService.delete(7, ME));
        verify(indicatorMapper, never()).deleteById(7);
    }

    @Test
    void allowsMaintainingOwnAndPublicIndicators() {
        when(indicatorMapper.findById(3)).thenReturn(indicator(ME));
        when(indicatorMapper.findById(4)).thenReturn(indicator(0L));
        when(indicatorMapper.update(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        assertDoesNotThrow(() -> indicatorService.update(3, new IndicatorUpdateRequest(), ME));
        assertDoesNotThrow(() -> indicatorService.update(4, new IndicatorUpdateRequest(), ME));
    }

    private ExamIndicator indicator(Long ownerId) {
        ExamIndicator ind = new ExamIndicator();
        ind.setId(7);
        ind.setOwnerId(ownerId);
        ind.setName("私有指标");
        return ind;
    }
}
