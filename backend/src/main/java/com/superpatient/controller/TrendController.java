package com.superpatient.controller;

import com.superpatient.dto.TrendVO;
import com.superpatient.dto.TrendableIndicatorVO;
import com.superpatient.exception.BusinessException;
import com.superpatient.pojo.Result;
import com.superpatient.service.TrendService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "趋势分析", description = "单个指标的历史变化")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    @Operation(summary = "指标趋势", description = "返回折线数据，每个点带该次报告的参考区间")
    @GetMapping("/trends")
    public Result trends(
            @Parameter(description = "指标ID") @RequestParam Integer indicatorId,
            @Parameter(description = "起始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "截止日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate) {
        if (indicatorId == null) throw new BusinessException("indicatorId 不能为空");
        TrendVO vo = trendService.getTrends(CurrentHolder.getCurrentId(), indicatorId, startDate, endDate);
        if (vo == null) return Result.error("指标不存在");
        return Result.success(vo);
    }

    @Operation(summary = "可查趋势的指标", description = "当前用户有数值记录的指标")
    @GetMapping("/trendable-indicators")
    public Result trendable() {
        List<TrendableIndicatorVO> list = trendService.getTrendableIndicators(CurrentHolder.getCurrentId());
        return Result.success(list);
    }
}
