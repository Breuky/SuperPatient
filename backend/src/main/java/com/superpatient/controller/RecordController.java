package com.superpatient.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.superpatient.dto.*;
import com.superpatient.pojo.PageResult;
import com.superpatient.pojo.Result;
import com.superpatient.service.PatientService;
import com.superpatient.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "检查记录", description = "检查记录的增删改查")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;
    private final PatientService patientService;

    @Operation(summary = "我的检查记录列表（分页）")
    @GetMapping("/records")
    public Result list(
            @Parameter(description = "按分类筛选") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "截止日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long patientId = patientService.getMyPatientId();
        PageHelper.startPage(page, pageSize);
        List<RecordListVO> list = recordService.list(patientId, categoryId, startDate, endDate);
        PageInfo<RecordListVO> pageInfo = new PageInfo<>(list);
        return Result.success(new PageResult<>(pageInfo.getTotal(), list));
    }

    @Operation(summary = "检查记录详情")
    @GetMapping("/records/{id}")
    public Result detail(@Parameter(description = "记录ID") @PathVariable Long id) {
        RecordDetailVO vo = recordService.detail(id);
        if (vo == null) return Result.error("检查记录不存在");
        return Result.success(vo);
    }

    @Operation(summary = "新增检查记录", description = "一次请求同时创建记录和所有指标值，后端自动计算 isAbnormal")
    @PostMapping("/records")
    public Result create(@RequestBody RecordCreateRequest request) {
        Long patientId = patientService.getMyPatientId();
        request.setPatientId(patientId);
        RecordCreateVO vo = recordService.create(request);
        return Result.success(vo);
    }

    @Operation(summary = "更新检查记录", description = "values 数组会全量覆盖原有值")
    @PutMapping("/records/{id}")
    public Result update(@Parameter(description = "记录ID") @PathVariable Long id,
                         @RequestBody RecordUpdateRequest request) {
        boolean updated = recordService.update(id, request);
        if (!updated) return Result.error("检查记录不存在");
        return Result.success(null);
    }

    @Operation(summary = "删除检查记录", description = "级联删除该记录下的所有指标值")
    @DeleteMapping("/records/{id}")
    public Result delete(@Parameter(description = "记录ID") @PathVariable Long id) {
        boolean deleted = recordService.delete(id);
        if (!deleted) return Result.error("检查记录不存在");
        return Result.success(null);
    }
}
