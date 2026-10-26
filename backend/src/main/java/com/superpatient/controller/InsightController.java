package com.superpatient.controller;

import com.superpatient.dto.InsightRequest;
import com.superpatient.dto.InsightVO;
import com.superpatient.pojo.Result;
import com.superpatient.ratelimit.RateLimit;
import com.superpatient.entity.InsightLog;
import com.superpatient.service.InsightService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "报告解读", description = "描述历史数据，不给医疗建议；模型不可用时自动降级为规则摘要")
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @Operation(summary = "解读一次检查",
            description = "mode=simple 证据包单次生成；mode=agent 由模型自行调用只读工具取数")
    @RateLimit(scope = "insight-daily", limit = 100, window = 1, unit = java.util.concurrent.TimeUnit.DAYS, failOpen = false)
    @RateLimit(scope = "insight-burst", limit = 10, window = 1, unit = java.util.concurrent.TimeUnit.MINUTES, failOpen = false)
    @PostMapping("/report")
    public Result report(@RequestBody InsightRequest request) {
        InsightVO vo = insightService.explain(request);
        return Result.success(vo);
    }

    @Operation(summary = "按题型统计", description = "哪类问题最多、最贵、最容易降级")
    @GetMapping("/stats")
    public Result stats() {
        return Result.success(insightService.categoryStats(CurrentHolder.getCurrentId()));
    }

    @Operation(summary = "我的解读记录", description = "最近的 Agent 调用留痕：问题、轮数、工具次数、token、是否降级")
    @GetMapping("/logs")
    public Result logs(@RequestParam(required = false) Integer limit) {
        List<InsightLog> logs = insightService.recentLogs(CurrentHolder.getCurrentId(), limit);
        return Result.success(logs);
    }
}
