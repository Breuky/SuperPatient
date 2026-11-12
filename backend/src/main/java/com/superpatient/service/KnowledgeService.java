package com.superpatient.service;

import com.superpatient.dto.KnowledgeVO;
import com.superpatient.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 知识库只读访问：只给已核对条目，检索与打分在 Agent 侧做 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;

    public List<KnowledgeVO> all() {
        return knowledgeMapper.findVerified();
    }
}
