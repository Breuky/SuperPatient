package com.superpatient.controller;

import com.superpatient.dto.UploadVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Tag(name = "文件上传", description = "报告照片上传与受控读取")
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

    /**
     * 读取报告图片。仅上传者本人可读；鉴权支持 token 请求头或 ?token= 查询参数
     * （浏览器 img 标签无法自定义请求头）。
     */
    @Operation(summary = "读取报告照片", description = "仅上传者本人可访问，需携带 token（header 或 query）")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@Parameter(description = "文件ID") @PathVariable Long id) throws IOException {
        FileService.FileContent content = fileService.load(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .body(content.resource());
    }
}
