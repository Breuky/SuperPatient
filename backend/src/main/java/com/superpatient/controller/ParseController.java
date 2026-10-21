package com.superpatient.controller;

import com.superpatient.dto.ExtractRequest;
import com.superpatient.dto.ExtractResultVO;
import com.superpatient.exception.BusinessException;
import com.superpatient.pojo.Result;
import com.superpatient.service.ReportExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "化验单抽取", description = "图片 → 结构化候选指标，不落库")
@RestController
@RequestMapping("/api/parse")
@RequiredArgsConstructor
public class ParseController {

    private final ReportExtractionService reportExtractionService;

    @Operation(summary = "解析化验单图片",
            description = "返回候选指标与未匹配项，前端校对后走 POST /api/records 落库")
    @PostMapping("/report")
    public Result parse(@RequestBody ExtractRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BusinessException("imageUrl 不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException("categoryId 不能为空");
        }
        ExtractResultVO vo = reportExtractionService.parse(request.getImageUrl(), request.getCategoryId());
        return Result.success(vo);
    }
}
