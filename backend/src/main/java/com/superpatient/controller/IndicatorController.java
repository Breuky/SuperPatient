package com.superpatient.controller;

import com.superpatient.dto.IndicatorCreateRequest;
import com.superpatient.dto.IndicatorDetailVO;
import com.superpatient.dto.IndicatorStatusRequest;
import com.superpatient.dto.IndicatorUpdateRequest;
import com.superpatient.pojo.Result;
import com.superpatient.service.IndicatorService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "指标管理", description = "检查指标的增删改查与启用/禁用")
@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;

    @Operation(summary = "指标列表", description = "默认仅返回启用状态的指标")
    @GetMapping
    public Result list(@Parameter(description = "按分类筛选") @RequestParam(required = false) Integer categoryId) {
        List<IndicatorDetailVO> list = indicatorService.list(categoryId, CurrentHolder.getCurrentId());
        return Result.success(list);
    }

    @Operation(summary = "新增指标")
    @PostMapping
    public Result create(@RequestBody IndicatorCreateRequest request) {
        IndicatorDetailVO vo = indicatorService.create(request, CurrentHolder.getCurrentId());
        return Result.success(vo);
    }

    @Operation(summary = "更新指标")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody IndicatorUpdateRequest request) {
        boolean updated = indicatorService.update(id, request, CurrentHolder.getCurrentId());
        if (!updated) return Result.error("指标不存在");
        return Result.success(null);
    }

    @Operation(summary = "禁用/启用指标", description = "软删除：将 status 设为 0（禁用）或 1（启用）")
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody IndicatorStatusRequest request) {
        boolean updated = indicatorService.updateStatus(id, request.getStatus(), CurrentHolder.getCurrentId());
        if (!updated) return Result.error("指标不存在");
        return Result.success(null);
    }

    @Operation(summary = "删除指标", description = "物理删除，仅无历史数据时可操作")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        try {
            indicatorService.delete(id, CurrentHolder.getCurrentId());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
        return Result.success(null);
    }
}
