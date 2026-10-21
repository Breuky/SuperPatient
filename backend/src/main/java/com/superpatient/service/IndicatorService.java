package com.superpatient.service;

import com.superpatient.dto.IndicatorCreateRequest;
import com.superpatient.dto.IndicatorDetailVO;
import com.superpatient.dto.IndicatorUpdateRequest;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.mapper.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final IndicatorMapper indicatorMapper;

    public List<IndicatorDetailVO> list(Integer categoryId) {
        List<ExamIndicator> indicators;
        if (categoryId != null) {
            indicators = indicatorMapper.findByCategoryId(categoryId);
        } else {
            indicators = indicatorMapper.findAllEnabled();
        }
        return indicators.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public IndicatorDetailVO create(IndicatorCreateRequest request) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setCategoryId(request.getCategoryId());
        indicator.setName(request.getName());
        indicator.setUnit(request.getUnit());
        indicator.setValueType(request.getValueType());
        indicator.setNormalMin(request.getNormalMin());
        indicator.setNormalMax(request.getNormalMax());
        indicator.setSortOrder(request.getSortOrder());
        indicatorMapper.insert(indicator);
        return toVO(indicatorMapper.findById(indicator.getId()));
    }

    @Transactional
    public boolean update(Integer id, IndicatorUpdateRequest request) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setId(id);
        indicator.setCategoryId(request.getCategoryId());
        indicator.setName(request.getName());
        indicator.setUnit(request.getUnit());
        indicator.setValueType(request.getValueType());
        indicator.setNormalMin(request.getNormalMin());
        indicator.setNormalMax(request.getNormalMax());
        indicator.setSortOrder(request.getSortOrder());
        return indicatorMapper.update(indicator) > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Integer status) {
        return indicatorMapper.updateStatus(id, status) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        if (indicatorMapper.existsValuesByIndicatorId(id)) {
            throw new RuntimeException("该指标已有历史数据，无法删除，请先禁用");
        }
        return indicatorMapper.deleteById(id) > 0;
    }

    private IndicatorDetailVO toVO(ExamIndicator ind) {
        IndicatorDetailVO vo = new IndicatorDetailVO();
        vo.setId(ind.getId());
        vo.setCategoryId(ind.getCategoryId());
        vo.setName(ind.getName());
        vo.setUnit(ind.getUnit());
        vo.setValueType(ind.getValueType());
        vo.setNormalMin(ind.getNormalMin());
        vo.setNormalMax(ind.getNormalMax());
        vo.setSortOrder(ind.getSortOrder());
        vo.setStatus(ind.getStatus());
        return vo;
    }
}
