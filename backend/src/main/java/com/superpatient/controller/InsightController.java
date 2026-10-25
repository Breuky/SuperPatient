package com.superpatient.controller;

import com.superpatient.dto.InsightRequest;
import com.superpatient.dto.InsightVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "报告解读", description = "描述历史数据，不给医疗建议；模型不可用时自动降级为规则摘要")
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @Operation(summary = "解读一次检查",
            description = "mode=simple 证据包单次生成；mode=agent 由模型自行调用只读工具取数")
    @PostMapping("/report")
    public Result report(@RequestBody InsightRequest request) {
        InsightVO vo = insightService.explain(request);
        return Result.success(vo);
    }
}
