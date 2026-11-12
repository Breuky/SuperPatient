package com.superpatient.mapper;

import com.superpatient.dto.KnowledgeVO;

import java.util.List;

public interface KnowledgeMapper {

    /** 只返回人工核对过的条目；未核对的不进检索 */
    List<KnowledgeVO> findVerified();
}
