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
        indicator.setAliases(normalizeAliases(request.getAliases()));
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
        // 传空字符串表示清空别名，传 null 表示不改
        indicator.setAliases(request.getAliases() == null ? null : normalizeAliases(request.getAliases()));
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

    /**
     * 别名归一：全角逗号/顿号/多空格都当分隔符，去空去重后统一用英文逗号拼接。
     * 匹配时 {@code IndicatorMatcher} 会再按逗号切分，这里只保证存储格式干净。
     */
    static String normalizeAliases(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("[,，、;；\\s]+");
        StringBuilder sb = new StringBuilder();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String part : parts) {
            String alias = part.trim();
            if (alias.isEmpty() || !seen.add(alias)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(alias);
        }
        String result = sb.toString();
        if (result.length() > 255) {
            throw new com.superpatient.exception.BusinessException("别名过长，请精简到 255 个字符以内");
        }
        return result;
    }

    private IndicatorDetailVO toVO(ExamIndicator ind) {
        IndicatorDetailVO vo = new IndicatorDetailVO();
        vo.setId(ind.getId());
        vo.setCategoryId(ind.getCategoryId());
        vo.setName(ind.getName());
        vo.setAliases(ind.getAliases());
        vo.setUnit(ind.getUnit());
        vo.setValueType(ind.getValueType());
        vo.setNormalMin(ind.getNormalMin());
        vo.setNormalMax(ind.getNormalMax());
        vo.setSortOrder(ind.getSortOrder());
        vo.setStatus(ind.getStatus());
        return vo;
    }
}
