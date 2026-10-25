package com.superpatient.dto;

import lombok.Data;

@Data
public class InsightRequest {
    private Long recordId;
    /** 用户的自然问题，agent 模式用它决定查什么 */
    private String question;
    /** simple（证据包单次生成）/ agent（工具调用循环），默认 simple */
    private String mode;
}
