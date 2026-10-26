package com.superpatient.controller;

import com.superpatient.dto.ExtractRequest;
import com.superpatient.dto.ExtractResultVO;
import com.superpatient.exception.BusinessException;
import com.superpatient.pojo.Result;
import com.superpatient.service.ReportExtractionService;
import com.superpatient.ratelimit.Dimension;
import com.superpatient.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "化验单抽取", description = "图片 → 结构化候选指标，不落库")
@RestController
@RequestMapping("/api/parse")
@RequiredArgsConstructor
public class ParseController {

    private final ReportExtractionService reportExtractionService;

    /** 全局默认值：请求里没传 doubleRead 时用它 */
    @Value("${app.extract.double-read.enabled:false}")
    private boolean doubleReadEnabled;

    @Operation(summary = "识别化验单（直接传图，不留存）",
            description = "multipart 上传，识别完即丢弃，不写磁盘也不建文件记录")
    @RateLimit(scope = "parse-daily", limit = 20, window = 1,
            unit = java.util.concurrent.TimeUnit.DAYS, failOpen = false)
    @RateLimit(scope = "parse-burst", limit = 3, window = 1,
            unit = java.util.concurrent.TimeUnit.MINUTES, failOpen = false)
    @PostMapping(value = "/report-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result parseImage(@RequestParam("file") MultipartFile file,
                             @RequestParam("categoryId") Integer categoryId,
                             @RequestParam(value = "doubleRead", required = false) Boolean doubleRead) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要识别的图片");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("图片不能超过 10MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean png = name.endsWith(".png");
        if (!png && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
            throw new BusinessException("仅支持 jpg/png 格式的图片");
        }
        boolean dr = doubleRead != null ? doubleRead : doubleReadEnabled;
        try {
            return Result.success(reportExtractionService.parseBytes(
                    file.getBytes(), png ? "image/png" : "image/jpeg", categoryId, dr));
        } catch (java.io.IOException e) {
            throw new BusinessException("读取上传图片失败: " + e.getMessage());
        }
    }

    @Operation(summary = "解析化验单图片",
            description = "返回候选指标与未匹配项，前端校对后走 POST /api/records 落库")
    @RateLimit(scope = "parse-daily", limit = 20, window = 1,
            unit = java.util.concurrent.TimeUnit.DAYS, failOpen = false)
    @RateLimit(scope = "parse-burst", limit = 3, window = 1,
            unit = java.util.concurrent.TimeUnit.MINUTES, failOpen = false)
    @PostMapping("/report")
    public Result parse(@RequestBody ExtractRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BusinessException("imageUrl 不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException("categoryId 不能为空");
        }
        boolean doubleRead = request.getDoubleRead() != null
                ? request.getDoubleRead()
                : doubleReadEnabled;
        ExtractResultVO vo = reportExtractionService.parse(
                request.getImageUrl(), request.getCategoryId(), doubleRead);
        return Result.success(vo);
    }
}
