package com.superpatient.service;

import com.superpatient.dto.*;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.entity.ExamRecord;
import com.superpatient.entity.ExamValue;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.CategoryMapper;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;
    private final UserMapper userMapper;
    private final IndicatorMapper indicatorMapper;
    private final CategoryMapper categoryMapper;
    private final ReferenceRangeService referenceRangeService;

    public List<RecordListVO> list(Long userId, Integer categoryId, String startDate, String endDate) {
        return recordMapper.page(userId, categoryId, startDate, endDate);
    }

    public RecordDetailVO detail(Long id) {
        ExamRecord record = recordMapper.findById(id);
        if (record == null) return null;
        checkOwnership(record);

        RecordDetailVO vo = new RecordDetailVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setCategoryId(record.getCategoryId());
        vo.setExamDate(record.getExamDate() != null ? record.getExamDate().toString() : null);
        vo.setHospital(record.getHospital());
        vo.setDoctor(record.getDoctor());
        vo.setNotes(record.getNotes());
        vo.setImageUrl(record.getImageUrl());

        ReferenceRangeService.UserContext ctx = ReferenceRangeService.UserContext.of(
                userMapper.findById(record.getUserId()), toLocalDate(record.getExamDate()));

        List<ExamValue> rawValues = valueMapper.findByRecordId(record.getId());
        List<Integer> indicatorIds = rawValues.stream().map(ExamValue::getIndicatorId).distinct().toList();
        var resolvedRanges = referenceRangeService.resolveBatch(indicatorIds, ctx);

        List<ValueVO> valueVOs = new ArrayList<>();
        for (ExamValue v : rawValues) {
            ExamIndicator ind = indicatorMapper.findById(v.getIndicatorId());
            ValueVO vvo = new ValueVO();
            vvo.setId(v.getId());
            vvo.setIndicatorId(v.getIndicatorId());
            vvo.setValueDecimal(v.getValueDecimal());
            vvo.setValueText(v.getValueText());
            vvo.setIsAbnormal(v.getIsAbnormal());
            vvo.setAbnormalText(toAbnormalText(v.getIsAbnormal()));
            if (ind != null) {
                vvo.setIndicatorName(ind.getName());
                vvo.setUnit(ind.getUnit());
                vvo.setValueType(ind.getValueType());
            }
            // 参考区间：报告快照 → 性别年龄规则 → 指标默认值
            boolean hasSnapshot = v.getRefMin() != null || v.getRefMax() != null;
            ReferenceRangeService.Resolved rule = resolvedRanges.get(v.getIndicatorId());
            if (hasSnapshot) {
                vvo.setNormalMin(v.getRefMin());
                vvo.setNormalMax(v.getRefMax());
                vvo.setRefSource(v.getRefSource() != null ? v.getRefSource() : "report");
            } else if (rule != null) {
                vvo.setNormalMin(rule.min());
                vvo.setNormalMax(rule.max());
                vvo.setRefSource("rule");
            } else if (ind != null) {
                vvo.setNormalMin(ind.getNormalMin());
                vvo.setNormalMax(ind.getNormalMax());
                vvo.setRefSource("default");
            }
            vvo.setRefText(v.getRefText());
            vvo.setRefMin(v.getRefMin());
            vvo.setRefMax(v.getRefMax());
            vvo.setRawName(v.getRawName());
            vvo.setRawUnit(v.getRawUnit());
            valueVOs.add(vvo);
        }

        if (record.getCategoryId() != null) {
            var category = categoryMapper.findById(record.getCategoryId());
            if (category != null) vo.setCategoryName(category.getName());
        }

        vo.setValues(valueVOs);
        return vo;
    }

    @Transactional
    public RecordCreateVO create(RecordCreateRequest request) {
        Long userId = CurrentHolder.getCurrentId();

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setCategoryId(request.getCategoryId());
        record.setExamDate(parseExamDate(request.getExamDate()));
        record.setHospital(request.getHospital());
        record.setDoctor(request.getDoctor());
        record.setNotes(request.getNotes());
        record.setImageUrl(request.getImageUrl());
        recordMapper.insert(record);

        ReferenceRangeService.UserContext ctx = ReferenceRangeService.UserContext.of(
                userMapper.findById(userId), toLocalDate(record.getExamDate()));
        List<ExamValue> values = buildValues(record.getId(), request.getValues(), ctx, userId);
        if (!values.isEmpty()) {
            valueMapper.batchInsert(values);
        }

        long abnormalCount = values.stream().filter(v -> v.getIsAbnormal() != 0).count();

        RecordCreateVO vo = new RecordCreateVO();
        vo.setId(record.getId());
        vo.setUserId(userId);
        vo.setCategoryId(record.getCategoryId());
        vo.setExamDate(record.getExamDate().toString());
        vo.setAbnormalCount((int) abnormalCount);

        List<ValueBriefVO> briefs = new ArrayList<>();
        for (ExamValue v : values) {
            ExamIndicator ind = indicatorMapper.findById(v.getIndicatorId());
            ValueBriefVO b = new ValueBriefVO();
            b.setIndicatorId(v.getIndicatorId());
            b.setIndicatorName(ind != null ? ind.getName() : null);
            b.setValueDecimal(v.getValueDecimal());
            b.setIsAbnormal(v.getIsAbnormal());
            b.setAbnormalText(toAbnormalText(v.getIsAbnormal()));
            briefs.add(b);
        }
        vo.setValues(briefs);

        return vo;
    }

    @Transactional
    public boolean update(Long id, RecordUpdateRequest request) {
        ExamRecord existing = recordMapper.findById(id);
        if (existing == null) return false;
        checkOwnership(existing);

        ExamRecord record = new ExamRecord();
        record.setId(id);
        if (request.getExamDate() != null && !request.getExamDate().isEmpty()) {
            record.setExamDate(parseExamDate(request.getExamDate()));
        }
        record.setHospital(request.getHospital());
        record.setDoctor(request.getDoctor());
        record.setNotes(request.getNotes());
        record.setImageUrl(request.getImageUrl());
        recordMapper.update(record);

        if (request.getValues() != null) {
            valueMapper.deleteByRecordId(id);
            LocalDateTime examDate = record.getExamDate() != null
                    ? record.getExamDate() : existing.getExamDate();
            ReferenceRangeService.UserContext ctx = ReferenceRangeService.UserContext.of(
                    userMapper.findById(existing.getUserId()), toLocalDate(examDate));
            List<ExamValue> values = buildValues(id, request.getValues(), ctx, existing.getUserId());
            if (!values.isEmpty()) {
                valueMapper.batchInsert(values);
            }
        }
        return true;
    }

    @Transactional
    public boolean delete(Long id) {
        ExamRecord existing = recordMapper.findById(id);
        if (existing == null) return false;
        checkOwnership(existing);
        return recordMapper.deleteById(id) > 0;
    }

    /**
     * 记录直接挂在用户上，归属校验不需要再查库。
     */
    private void checkOwnership(ExamRecord record) {
        if (!Objects.equals(record.getUserId(), CurrentHolder.getCurrentId())) {
            throw new BusinessException("无权访问该检查记录");
        }
    }

    private List<ExamValue> buildValues(Long recordId, List<ValueItem> items,
                                        ReferenceRangeService.UserContext ctx, Long userId) {
        List<ExamValue> values = new ArrayList<>();
        if (items == null) return values;

        // 一次查回全部指标定义 + 一次算完全部区间：20 个指标从 41 次查询降到 3 次
        List<Integer> ids = items.stream().map(ValueItem::getIndicatorId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Integer, ExamIndicator> pool = ids.isEmpty() ? Map.of()
                : indicatorMapper.findByIds(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(ExamIndicator::getId, it -> it));
        Map<Integer, ReferenceRangeService.Resolved> resolved = ids.isEmpty() ? Map.of()
                : referenceRangeService.resolveBatch(ids, ctx);

        for (ValueItem item : items) {
            ExamIndicator ind = pool.get(item.getIndicatorId());
            if (ind == null) {
                // 静默跳过会让用户以为「保存成功」，实际少了一项
                throw new BusinessException("指标不存在: " + item.getIndicatorId());
            }
            if (ind.getStatus() != null && ind.getStatus() != 1) {
                throw new BusinessException("指标已停用: " + ind.getName());
            }
            // 私有指标只能挂到 owner 自己的记录上，否则拒绝整条请求
            if (!ind.isVisibleTo(userId)) {
                throw new BusinessException("指标不存在或无权使用: " + item.getIndicatorId());
            }

            ExamValue v = new ExamValue();
            v.setRecordId(recordId);
            v.setIndicatorId(item.getIndicatorId());
            v.setValueDecimal(item.getValueDecimal());
            v.setValueText(item.getValueText());
            v.setRefText(item.getRefText());
            v.setRawName(item.getRawName());
            v.setRawUnit(item.getRawUnit());

            ReferenceRangeService.Resolved range = effectiveRange(item, ind, resolved.get(ind.getId()));
            // 报告自带或规则推算出来的区间都落快照（规则来源可能是 "rule"，
            // 也可能带具体出处如 "WS/T 405-2012"）；指标默认值不冻结
            if (range.source() != null && !"default".equals(range.source())) {
                v.setRefMin(range.min());
                v.setRefMax(range.max());
                v.setRefSource(range.source());
            }
            v.setIsAbnormal(calcAbnormal(item.getValueDecimal(), range.min(), range.max()));
            values.add(v);
        }
        return values;
    }

    /**
     * 生效区间：请求里带的报告区间 &gt; 规则表 &gt; 指标默认值
     */
    private ReferenceRangeService.Resolved effectiveRange(ValueItem item, ExamIndicator ind,
                                                          ReferenceRangeService.Resolved preResolved) {
        if (item.getRefMin() != null || item.getRefMax() != null) {
            // 带上来源：报告没印区间时，候选里的区间其实是规则算的（source=rule）
            String source = item.getRefSource() != null && !item.getRefSource().isBlank()
                    ? item.getRefSource() : "report";
            return new ReferenceRangeService.Resolved(item.getRefMin(), item.getRefMax(),
                    item.getRefText(), source);
        }
        if (preResolved != null) {
            return preResolved;
        }
        return new ReferenceRangeService.Resolved(ind.getNormalMin(), ind.getNormalMax(), null, "default");
    }

    /**
     * 根据参考区间和实际值计算 isAbnormal：0=正常 1=偏高 2=偏低
     */
    private int calcAbnormal(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return 0;
        if (max != null && value.compareTo(max) > 0) return 1;
        if (min != null && value.compareTo(min) < 0) return 2;
        return 0;
    }

    /**
     * 检查日期是必填：库里 exam_date 是 NOT NULL，但请求体为空时
     * LocalDateTime.parse 会抛 DateTimeParseException，被兜底成「服务器内部错误」。
     * 这里提前拦成业务提示。
     */
    private static LocalDateTime parseExamDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("请选择检查日期");
        }
        LocalDateTime date;
        try {
            date = LocalDateTime.parse(raw);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException("检查日期格式不正确，应为 2026-03-28T09:30");
        }
        if (date.toLocalDate().isAfter(LocalDate.now())) {
            throw new BusinessException("检查日期不能晚于今天");
        }
        return date;
    }

    /** 参考区间按「检查当天」的年龄算，不能用今天：补录历史单据会跨年龄段 */
    private static LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private String toAbnormalText(int isAbnormal) {
        return switch (isAbnormal) {
            case 1 -> "偏高";
            case 2 -> "偏低";
            default -> "正常";
        };
    }
}