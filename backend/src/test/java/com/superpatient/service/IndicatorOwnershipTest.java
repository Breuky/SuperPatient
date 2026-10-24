package com.superpatient.service;

import com.superpatient.dto.IndicatorCreateRequest;
import com.superpatient.dto.IndicatorDetailVO;
import com.superpatient.dto.IndicatorUpdateRequest;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.IndicatorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指标分公共与私有：公共只读，私有指标只有 owner 能维护，别人的私有指标表现得像不存在
 */
@ExtendWith(MockitoExtension.class)
class IndicatorOwnershipTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;

    @Mock private IndicatorMapper indicatorMapper;

    @InjectMocks private IndicatorService indicatorService;

    @Test
    void publicIndicatorIsVisibleToEveryone() {
        assertTrue(indicator(ExamIndicator.SYSTEM_OWNER).isVisibleTo(ME));
        assertTrue(indicator(ExamIndicator.SYSTEM_OWNER).isVisibleTo(OTHER));
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
        verify(indicatorMapper, never()).update(any());
    }

    @Test
    void cannotDeleteAnotherUsersPrivateIndicator() {
        when(indicatorMapper.findById(7)).thenReturn(indicator(OTHER));

        assertThrows(BusinessException.class, () -> indicatorService.delete(7, ME));
        verify(indicatorMapper, never()).deleteById(7);
    }

    @Test
    void publicIndicatorIsReadOnly() {
        when(indicatorMapper.findById(4)).thenReturn(indicator(ExamIndicator.SYSTEM_OWNER));

        assertThrows(BusinessException.class,
                () -> indicatorService.update(4, new IndicatorUpdateRequest(), ME));
        assertThrows(BusinessException.class, () -> indicatorService.updateStatus(4, 0, ME));
        assertThrows(BusinessException.class, () -> indicatorService.delete(4, ME));
    }

    @Test
    void allowsMaintainingOwnIndicator() {
        when(indicatorMapper.findById(3)).thenReturn(indicator(ME));
        when(indicatorMapper.update(any())).thenReturn(1);

        assertDoesNotThrow(() -> indicatorService.update(3, new IndicatorUpdateRequest(), ME));
        assertDoesNotThrow(() -> indicatorService.updateStatus(3, 0, ME));
    }

    @Test
    void newIndicatorBelongsToCreator() {
        when(indicatorMapper.existsByName(1, "大血小板比率", ME, null)).thenReturn(false);
        when(indicatorMapper.findById(any())).thenReturn(indicator(ME));

        IndicatorCreateRequest request = new IndicatorCreateRequest();
        request.setCategoryId(1);
        request.setName("大血小板比率");

        IndicatorDetailVO vo = indicatorService.create(request, ME);

        ArgumentCaptor<ExamIndicator> captor = ArgumentCaptor.forClass(ExamIndicator.class);
        verify(indicatorMapper).insert(captor.capture());
        assertEquals(ME, captor.getValue().getOwnerId(), "新增指标应归属创建者，而不是公共库");
        assertEquals(ME, vo.getOwnerId());
    }

    @Test
    void rejectsDuplicateNameAgainstPublicLibrary() {
        when(indicatorMapper.existsByName(1, "血红蛋白", ME, null)).thenReturn(true);

        IndicatorCreateRequest request = new IndicatorCreateRequest();
        request.setCategoryId(1);
        request.setName("血红蛋白");

        assertThrows(BusinessException.class, () -> indicatorService.create(request, ME));
        verify(indicatorMapper, never()).insert(any());
    }

    private ExamIndicator indicator(Long ownerId) {
        ExamIndicator ind = new ExamIndicator();
        ind.setId(7);
        ind.setOwnerId(ownerId);
        ind.setName("私有指标");
        return ind;
    }
}
