package com.superpatient.controller;

import com.superpatient.dto.ReferenceRangeRequest;
import com.superpatient.dto.ReferenceRangeVO;
import com.superpatient.exception.BusinessException;
import com.superpatient.pojo.Result;
import com.superpatient.service.ReferenceRangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "参考区间规则", description = "按性别/年龄维护指标的参考区间")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceRangeController {

    private final ReferenceRangeService referenceRangeService;

    @Operation(summary = "某指标下的参考区间规则")
    @GetMapping("/indicators/{indicatorId}/reference-ranges")
    public Result list(@PathVariable Integer indicatorId) {
        List<ReferenceRangeVO> list = referenceRangeService.list(indicatorId);
        return Result.success(list);
    }

    @Operation(summary = "新增参考区间规则")
    @PostMapping("/reference-ranges")
    public Result create(@RequestBody ReferenceRangeRequest request) {
        if (request.getIndicatorId() == null) throw new BusinessException("indicatorId 不能为空");
        if (request.getAgeMin() != null && request.getAgeMax() != null
                && request.getAgeMin() > request.getAgeMax()) {
            throw new BusinessException("年龄下限不能大于上限");
        }
        if (request.getRefMin() != null && request.getRefMax() != null
                && request.getRefMin().compareTo(request.getRefMax()) > 0) {
            throw new BusinessException("参考下限不能大于上限");
        }
        return Result.success(referenceRangeService.create(request));
    }

    @Operation(summary = "更新参考区间规则")
    @PutMapping("/reference-ranges/{id}")
    public Result update(@PathVariable Integer id, @RequestBody ReferenceRangeRequest request) {
        if (referenceRangeService.update(id, request)) return Result.success(null);
        return Result.error("规则不存在");
    }

    @Operation(summary = "删除参考区间规则")
    @DeleteMapping("/reference-ranges/{id}")
    public Result delete(@PathVariable Integer id) {
        if (referenceRangeService.delete(id)) return Result.success(null);
        return Result.error("规则不存在");
    }
}
