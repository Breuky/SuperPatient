package com.superpatient.dto;

import lombok.Data;

/** 知识条目：RAG 语料的一条，必须能通过 sourceId + locator 回到原文 */
@Data
public class KnowledgeVO {
    private Integer id;
    private Integer indicatorId;
    private String indicatorName;
    private String aliases;
    private String field;
    private String text;
    private String sourceId;
    private String sourceTitle;
    private String locator;
}
