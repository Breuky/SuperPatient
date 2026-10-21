package com.superpatient.controller;

import com.superpatient.dto.TrendVO;
import com.superpatient.dto.TrendableIndicatorVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.PatientService;
import com.superpatient.service.TrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "趋势分析", description = "指标变化趋势图表数据")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;
    private final PatientService patientService;

    @Operation(summary = "获取指标趋势数据", description = "当前每次查询单个指标的趋势")
    @GetMapping("/trends")
    public Result getTrends(
            @Parameter(description = "指标ID") @RequestParam Integer indicatorId,
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "截止日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate) {
        Long patientId = patientService.getMyPatientId();
        TrendVO vo = trendService.getTrends(patientId, indicatorId, startDate, endDate);
        if (vo == null) return Result.error("指标不存在");
        return Result.success(vo);
    }

    @Operation(summary = "获取可查趋势的指标列表", description = "返回当前用户有历史数据的所有数值型指标")
    @GetMapping("/trendable-indicators")
    public Result getTrendableIndicators() {
        Long patientId = patientService.getMyPatientId();
        List<TrendableIndicatorVO> list = trendService.getTrendableIndicators(patientId);
        return Result.success(list);
    }
}
