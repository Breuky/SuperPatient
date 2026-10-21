package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上传文件归属记录：用于把图片访问收敛到鉴权接口，而不是公开静态目录。
 */
@Data
public class UploadFile {
    private Long id;
    private Long userId;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String bizType;
    private LocalDateTime createdAt;
}
