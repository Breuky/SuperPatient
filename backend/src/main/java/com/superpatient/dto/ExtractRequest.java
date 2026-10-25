package com.superpatient.dto;

import lombok.Data;

@Data
public class ExtractRequest {
    /** 上传接口返回的图片地址，如 /api/files/12 */
    private String imageUrl;
    private Integer categoryId;
    /** 精读：同一张图识别两次并比对，只对不一致的行仲裁。耗时和成本约 2-3 倍 */
    private Boolean doubleRead;
}
