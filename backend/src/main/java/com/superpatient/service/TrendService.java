package com.superpatient.service;

import com.superpatient.dto.TrendVO;
import com.superpatient.dto.TrendableIndicatorVO;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.entity.ExamValue;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendService {

    private final ValueMapper valueMapper;
    private final IndicatorMapper indicatorMapper;
    private final UserMapper userMapper;
    private final ReferenceRangeService referenceRangeService;

    public TrendVO getTrends(Long userId, Integer indicatorId, String startDate, String endDate) {
        ExamIndicator ind = indicatorMapper.findById(indicatorId);
        // 别人的私有指标当作不存在，避免拿到别人的指标名与参考区间
        if (ind == null || !ind.isVisibleTo(userId)) return null;

        List<ExamValue> values = valueMapper.findByUserIdAndIndicatorId(
                userId, indicatorId, startDate, endDate);

        ReferenceRangeService.UserContext ctx = ReferenceRangeService.UserContext.of(
                userMapper.findById(userId));
        ReferenceRangeService.Resolved rule = referenceRangeService.resolve(ind.getId(), ctx);

        TrendVO vo = new TrendVO();
        TrendVO.TrendIndicator ti = new TrendVO.TrendIndicator();
        ti.setId(ind.getId());
        ti.setName(ind.getName());
        ti.setUnit(ind.getUnit());
        // 指标级默认带性别年龄规则，前端在点没有快照时用它兜底
        ti.setNormalMin(rule != null ? rule.min() : ind.getNormalMin());
        ti.setNormalMax(rule != null ? rule.max() : ind.getNormalMax());
        vo.setIndicator(ti);

        List<TrendVO.TrendPoint> points = new ArrayList<>();
        for (ExamValue v : values) {
            TrendVO.TrendPoint p = new TrendVO.TrendPoint();
            p.setRecordId(v.getRecordId());
            p.setValue(v.getValueDecimal());
            p.setIsAbnormal(v.getIsAbnormal());
            p.setExamDate(v.getExamDate() != null
                    ? v.getExamDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : null);
            boolean hasSnapshot = v.getRefMin() != null || v.getRefMax() != null;
            p.setRefMin(hasSnapshot ? v.getRefMin() : (rule != null ? rule.min() : null));
            p.setRefMax(hasSnapshot ? v.getRefMax() : (rule != null ? rule.max() : null));
            p.setRefText(v.getRefText());
            points.add(p);
        }
        vo.setPoints(points);
        return vo;
    }

    public List<TrendableIndicatorVO> getTrendableIndicators(Long userId) {
        List<ExamIndicator> allNumeric = indicatorMapper.findAllEnabled(userId).stream()
                .filter(i -> "number".equals(i.getValueType()))
                .toList();

        List<TrendableIndicatorVO> result = new ArrayList<>();
        for (ExamIndicator ind : allNumeric) {
            List<ExamValue> values = valueMapper.findByUserIdAndIndicatorId(
                    userId, ind.getId(), null, null);
            if (!values.isEmpty()) {
                TrendableIndicatorVO vo = new TrendableIndicatorVO();
                vo.setIndicatorId(ind.getId());
                vo.setIndicatorName(ind.getName());
                vo.setUnit(ind.getUnit());
                vo.setCategoryId(ind.getCategoryId());
                vo.setRecordCount(values.size());
                result.add(vo);
            }
        }
        return result;
    }
}
