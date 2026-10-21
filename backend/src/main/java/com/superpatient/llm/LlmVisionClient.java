package com.superpatient.llm;

/**
 * 多模态模型客户端：输入图片 + 提示词，返回模型输出的 JSON 文本。
 * 刻意只返回字符串，schema 校验与归一化留给后处理层，
 * 便于做 prompt / schema 约束 / 后处理 的对照评测。
 */
public interface LlmVisionClient {

    String extract(byte[] image, String mimeType, String prompt);

    /** 用于在结果里标注实际使用的引擎 */
    String displayName();
}
