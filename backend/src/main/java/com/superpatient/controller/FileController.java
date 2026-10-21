package com.superpatient.controller;

import com.superpatient.dto.UploadVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传", description = "报告照片上传")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传报告照片", description = "支持 jpg/png，最大 10MB")
    @PostMapping("/upload")
    public Result upload(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件类型: report=检查报告, avatar=头像") @RequestParam(required = false) String type) {
        try {
            UploadVO vo = fileService.upload(file, type);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
