package com.superpatient.service;

import com.superpatient.dto.IndicatorVO;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.mapper.FocusIndicatorMapper;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 长期关注指标：用户把自己关心的指标标星，录入页与趋势页优先展示。
 */
@Service
@RequiredArgsConstructor
public class FocusIndicatorService {

    private final FocusIndicatorMapper focusIndicatorMapper;
    private final IndicatorMapper indicatorMapper;

    public List<Integer> listIds() {
        return focusIndicatorMapper.findIndicatorIdsByUserId(CurrentHolder.getCurrentId());
    }

    public List<IndicatorVO> list() {
        List<IndicatorVO> result = new ArrayList<>();
        for (Integer id : listIds()) {
            ExamIndicator ind = indicatorMapper.findById(id);
            if (ind == null || ind.getStatus() == null || ind.getStatus() != 1) continue;
            IndicatorVO vo = new IndicatorVO();
            vo.setId(ind.getId());
            vo.setCategoryId(ind.getCategoryId());
            vo.setName(ind.getName());
            vo.setUnit(ind.getUnit());
            vo.setValueType(ind.getValueType());
            vo.setNormalMin(ind.getNormalMin());
            vo.setNormalMax(ind.getNormalMax());
            vo.setSortOrder(ind.getSortOrder());
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public void add(Integer indicatorId) {
        focusIndicatorMapper.insert(CurrentHolder.getCurrentId(), indicatorId);
    }

    @Transactional
    public void remove(Integer indicatorId) {
        focusIndicatorMapper.delete(CurrentHolder.getCurrentId(), indicatorId);
    }
}
