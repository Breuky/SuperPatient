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
    /** 来源等级 A=国标/行标 B=指南共识 C=教材 D=项目自述 */
    private String originLevel;
    private String locator;
}