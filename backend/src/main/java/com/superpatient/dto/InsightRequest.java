package com.superpatient.dto;

import lombok.Data;

import java.util.List;

@Data
public class InsightRequest {
    private Long recordId;
    /** 用户的自然问题，agent 模式用它决定查什么 */
    private String question;
    /** simple（证据包单次生成）/ agent（工具调用循环），默认 agent */
    private String mode;
    /** 同一会话里之前的问答，用于「那上次呢」这类追问 */
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        /** user / assistant */
        private String role;
        private String content;
    }
}
