package com.superpatient.service;

import com.superpatient.dto.*;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.entity.ExamRecord;
import com.superpatient.entity.ExamValue;
import com.superpatient.entity.Patient;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.CategoryMapper;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.PatientMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;
    private final PatientMapper patientMapper;
    private final IndicatorMapper indicatorMapper;
    private final CategoryMapper categoryMapper;

    public List<RecordListVO> list(Long patientId, Integer categoryId, String startDate, String endDate) {
        return recordMapper.page(patientId, categoryId, startDate, endDate);
    }

    public RecordDetailVO detail(Long id) {
        ExamRecord record = recordMapper.findById(id);
        if (record == null) return null;
        checkOwnership(record);

        RecordDetailVO vo = new RecordDetailVO();
        vo.setId(record.getId());
        vo.setPatientId(record.getPatientId());
        vo.setCategoryId(record.getCategoryId());
        vo.setExamDate(record.getExamDate() != null ? record.getExamDate().toString() : null);
        vo.setHospital(record.getHospital());
        vo.setDoctor(record.getDoctor());
        vo.setNotes(record.getNotes());
        vo.setImageUrl(record.getImageUrl());

        List<ExamValue> rawValues = valueMapper.findByRecordId(record.getId());
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
            // 参考区间以本次报告为准，指标库的值只是默认值
            vvo.setNormalMin(v.getRefMin() != null ? v.getRefMin()
                    : (ind != null ? ind.getNormalMin() : null));
            vvo.setNormalMax(v.getRefMax() != null ? v.getRefMax()
                    : (ind != null ? ind.getNormalMax() : null));
            vvo.setRefText(v.getRefText());
            vvo.setRawName(v.getRawName());
            valueVOs.add(vvo);
        }

        // Category name
        if (record.getCategoryId() != null) {
            var category = categoryMapper.findById(record.getCategoryId());
            if (category != null) vo.setCategoryName(category.getName());
        }
        // Patient name
        var patient = patientMapper.findById(record.getPatientId());
        if (patient != null) vo.setPatientName(patient.getName());

        vo.setValues(valueVOs);
        return vo;
    }

    @Transactional
    public RecordCreateVO create(RecordCreateRequest request) {
        ExamRecord record = new ExamRecord();
        record.setPatientId(request.getPatientId());
        record.setCategoryId(request.getCategoryId());
        record.setExamDate(LocalDateTime.parse(request.getExamDate()));
        record.setHospital(request.getHospital());
        record.setDoctor(request.getDoctor());
        record.setNotes(request.getNotes());
        record.setImageUrl(request.getImageUrl());
        recordMapper.insert(record);

        List<ExamValue> values = buildValues(record.getId(), request.getCategoryId(), request.getValues());
        if (!values.isEmpty()) {
            valueMapper.batchInsert(values);
        }

        long abnormalCount = values.stream().filter(v -> v.getIsAbnormal() != 0).count();

        RecordCreateVO vo = new RecordCreateVO();
        vo.setId(record.getId());
        vo.setPatientId(record.getPatientId());
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
            record.setExamDate(LocalDateTime.parse(request.getExamDate()));
        }
        record.setHospital(request.getHospital());
        record.setDoctor(request.getDoctor());
        record.setNotes(request.getNotes());
        record.setImageUrl(request.getImageUrl());
        recordMapper.update(record);

        if (request.getValues() != null) {
            valueMapper.deleteByRecordId(id);
            List<ExamValue> values = buildValues(id, existing.getCategoryId(), request.getValues());
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
     * 校验记录归属：只能操作自己名下的患者数据。
     * 修复越权访问（此前任意登录用户可通过 id 读取/修改他人的检查记录）。
     */
    private void checkOwnership(ExamRecord record) {
        Patient patient = patientMapper.findById(record.getPatientId());
        if (patient == null || !Objects.equals(patient.getUserId(), CurrentHolder.getCurrentId())) {
            throw new BusinessException("无权访问该检查记录");
        }
    }

    private List<ExamValue> buildValues(Long recordId, Integer categoryId, List<ValueItem> items) {
        List<ExamValue> values = new ArrayList<>();
        if (items == null) return values;

        for (ValueItem item : items) {
            ExamIndicator ind = indicatorMapper.findById(item.getIndicatorId());
            if (ind == null) continue;

            ExamValue v = new ExamValue();
            v.setRecordId(recordId);
            v.setIndicatorId(item.getIndicatorId());
            v.setValueDecimal(item.getValueDecimal());
            v.setValueText(item.getValueText());
            v.setRefMin(item.getRefMin());
            v.setRefMax(item.getRefMax());
            v.setRefText(item.getRefText());
            v.setRawName(item.getRawName());
            v.setRawUnit(item.getRawUnit());
            v.setIsAbnormal(calcAbnormal(item.getValueDecimal(),
                    firstNonNull(item.getRefMin(), ind.getNormalMin()),
                    firstNonNull(item.getRefMax(), ind.getNormalMax())));
            values.add(v);
        }
        return values;
    }

    /**
     * 根据参考区间和实际值计算 isAbnormal：
     * 0=正常 1=偏高 2=偏低
     * 区间优先取本次报告印刷值，缺省时回落指标库默认值。
     */
    private int calcAbnormal(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return 0;
        if (max != null && value.compareTo(max) > 0) return 1;
        if (min != null && value.compareTo(min) < 0) return 2;
        return 0;
    }

    private BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }

    private String toAbnormalText(int isAbnormal) {
        return switch (isAbnormal) {
            case 1 -> "偏高";
            case 2 -> "偏低";
            default -> "正常";
        };
    }
}
