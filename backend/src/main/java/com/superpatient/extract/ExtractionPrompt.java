package com.superpatient.extract;

import com.superpatient.entity.ExamIndicator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 抽取提示词。把指标库的规范名喂给模型，减少同义写法带来的召回损失。
 */
public final class ExtractionPrompt {

    private ExtractionPrompt() {
    }

    public static String build(List<ExamIndicator> indicators) {
        String known = indicators == null || indicators.isEmpty()
                ? "（无）"
                : indicators.stream().map(ExamIndicator::getName).collect(Collectors.joining("、"));

        return """
                你是医学检验报告的结构化助手。请识别图片中的所有指标行，只输出 JSON，不要输出任何解释、不要使用 Markdown 代码块。

                输出格式：
                {"examDate":"YYYY-MM-DD 或 null","hospital":"医院名或 null","items":[{"name":"原始指标名","value":"原始结果","unit":"原始单位或 null","refText":"参考区间原文或 null","refMin":数字或 null,"refMax":数字或 null}]}

                要求：
                1. 逐行抽取，不要遗漏，一行一个指标；不要合并、不要推测报告中不存在的内容。
                1.1 严格按照图片上从上到下、从左到右的阅读顺序输出，不要重排、不要按名称分组。
                2. name 保留报告上的写法，不要自行改成规范名。
                3. value 保留原文（如 "<0.04"、"阴性"、"3.2↑"），不要做单位换算。
                4. refText 填参考区间原文；能解析出上下限时同时填 refMin/refMax，解析不出就填 null。
                5. 看不清的字段填 null，不要编造。
                6. 如果指标名与下列规范名含义相同，优先保留报告原文（归一化由后端完成）：%s
                """.formatted(known);
    }
}
