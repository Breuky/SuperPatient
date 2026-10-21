package com.superpatient.dto;

import lombok.Data;

@Data
public class ExtractRequest {
    /** 上传接口返回的图片地址，如 /api/files/12 */
    private String imageUrl;
    private Integer categoryId;
}
