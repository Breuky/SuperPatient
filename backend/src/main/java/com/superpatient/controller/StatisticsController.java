package com.superpatient.controller;

import com.superpatient.dto.OverviewVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "统计", description = "当前用户的宏观统计，供 AI 助手与概览使用")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "全部记录概览",
            description = "总次数、时间范围、各分类次数、异常最多的指标；范围固定为当前登录用户")
    @GetMapping("/overview")
    public Result overview() {
        return Result.success(statisticsService.overview());
    }
}
