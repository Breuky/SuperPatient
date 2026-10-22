package com.superpatient.controller;

import com.superpatient.dto.IndicatorVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.FocusIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "关注指标", description = "长期关注的指标，录入与趋势优先展示")
@RestController
@RequestMapping("/api/focus-indicators")
@RequiredArgsConstructor
public class FocusIndicatorController {

    private final FocusIndicatorService focusIndicatorService;

    @Operation(summary = "我的关注指标", description = "返回 id 列表与指标详情")
    @GetMapping
    public Result list() {
        List<IndicatorVO> list = focusIndicatorService.list();
        return Result.success(list);
    }

    @Operation(summary = "关注某指标")
    @PostMapping("/{indicatorId}")
    public Result add(@Parameter(description = "指标ID") @PathVariable Integer indicatorId) {
        focusIndicatorService.add(indicatorId);
        return Result.success(null);
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{indicatorId}")
    public Result remove(@Parameter(description = "指标ID") @PathVariable Integer indicatorId) {
        focusIndicatorService.remove(indicatorId);
        return Result.success(null);
    }
}
