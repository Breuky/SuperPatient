package com.superpatient.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 报告解读结果。免责声明由服务端拼接，不交给模型生成 */
@Data
public class InsightVO {

    /** simple / agent / rule（模型不可用时的降级） */
    private String mode;
    private String summary;
    private List<Highlight> highlights = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    /** agent 模式下了几轮 */
    private Integer turns;
    /** 每次工具调用的轨迹，agent 模式才有 */
    private List<TraceItem> trace = new ArrayList<>();
    private boolean degraded;
    private String disclaimer;
    /** 模型 token 用量，仅用于对照实验 */
    private Map<String, Object> usage;

    @Data
    public static class Highlight {
        private String indicator;
        private String text;
        private String basis;
    }

    @Data
    public static class TraceItem {
        private String tool;
        private Map<String, Object> args;
        private Integer ms;
        private Boolean ok;
    }
}
