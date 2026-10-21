package com.superpatient.service;

import com.superpatient.dto.PatientCreateRequest;
import com.superpatient.dto.PatientDetailVO;
import com.superpatient.dto.PatientListVO;
import com.superpatient.dto.PatientUpdateRequest;
import com.superpatient.entity.Patient;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.PatientMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientMapper patientMapper;
    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;

    public List<PatientListVO> list(String keyword) {
        Long userId = CurrentHolder.getCurrentId();
        return patientMapper.page(userId, keyword);
    }

    public PatientDetailVO detail(Long id) {
        Patient patient = patientMapper.findById(id);
        if (patient == null) return null;
        checkOwnership(patient);

        PatientDetailVO vo = new PatientDetailVO();
        vo.setId(patient.getId());
        vo.setName(patient.getName());
        vo.setGender(patient.getGender());
        vo.setGenderText(patient.getGender() == 0 ? "女" : "男");
        if (patient.getBirthDate() != null) {
            vo.setBirthDate(patient.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            vo.setAge((int) ChronoUnit.YEARS.between(patient.getBirthDate(), LocalDate.now()));
        }
        if (patient.getDiagnosisDate() != null) {
            vo.setDiagnosisDate(patient.getDiagnosisDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        vo.setPhone(patient.getPhone());
        vo.setNotes(patient.getNotes());

        PatientDetailVO.PatientStats stats = new PatientDetailVO.PatientStats();
        List<PatientDetailVO.CategoryStat> categoryStats = recordMapper.getCategoryStats(id);
        int totalExams = categoryStats.stream().mapToInt(PatientDetailVO.CategoryStat::getCount).sum();
        stats.setTotalExams(totalExams);
        stats.setLastExamDate(categoryStats.stream()
                .map(PatientDetailVO.CategoryStat::getLastDate)
                .max(String::compareTo).orElse(null));
        stats.setCategoryStats(categoryStats);
        vo.setStats(stats);

        return vo;
    }

    @Transactional
    public Patient create(PatientCreateRequest request) {
        Patient patient = new Patient();
        patient.setUserId(CurrentHolder.getCurrentId());
        patient.setName(request.getName());
        patient.setGender(request.getGender());
        if (request.getBirthDate() != null && !request.getBirthDate().isEmpty()) {
            patient.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }
        if (request.getDiagnosisDate() != null && !request.getDiagnosisDate().isEmpty()) {
            patient.setDiagnosisDate(LocalDate.parse(request.getDiagnosisDate()));
        }
        patient.setPhone(request.getPhone());
        patient.setNotes(request.getNotes());
        patientMapper.insert(patient);
        return patient;
    }

    @Transactional
    public boolean update(Long id, PatientUpdateRequest request) {
        Patient existing = patientMapper.findById(id);
        if (existing == null) return false;
        checkOwnership(existing);

        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(request.getName());
        patient.setGender(request.getGender());
        if (request.getBirthDate() != null && !request.getBirthDate().isEmpty()) {
            patient.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }
        if (request.getDiagnosisDate() != null && !request.getDiagnosisDate().isEmpty()) {
            patient.setDiagnosisDate(LocalDate.parse(request.getDiagnosisDate()));
        }
        patient.setPhone(request.getPhone());
        patient.setNotes(request.getNotes());
        return patientMapper.update(patient) > 0;
    }

    public Long getMyPatientId() {
        Long userId = CurrentHolder.getCurrentId();
        Patient patient = patientMapper.findByUserId(userId);
        if (patient == null) {
            patient = new Patient();
            patient.setUserId(userId);
            patient.setName("患者");
            patient.setGender(1);
            patientMapper.insert(patient);
        }
        return patient.getId();
    }

    public PatientDetailVO getMyPatient() {
        Long patientId = getMyPatientId();
        return detail(patientId);
    }

    @Transactional
    public boolean updateMyPatient(PatientUpdateRequest request) {
        Long patientId = getMyPatientId();
        return update(patientId, request);
    }

    @Transactional
    public boolean delete(Long id) {
        Patient existing = patientMapper.findById(id);
        if (existing == null) return false;
        checkOwnership(existing);
        return patientMapper.deleteById(id) > 0;
    }

    /**
     * 校验患者归属：只能操作自己名下的患者。
     */
    private void checkOwnership(Patient patient) {
        if (!Objects.equals(patient.getUserId(), CurrentHolder.getCurrentId())) {
            throw new BusinessException("无权访问该患者");
        }
    }
}
