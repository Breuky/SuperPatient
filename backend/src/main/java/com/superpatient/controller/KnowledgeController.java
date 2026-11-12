package com.superpatient.controller;

import com.superpatient.dto.KnowledgeVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "知识条目")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "全部已核对知识条目", description = "供 Agent 建检索索引；未核对的条目不会返回")
    @GetMapping
    public Result all() {
        List<KnowledgeVO> list = knowledgeService.all();
        return Result.success(list);
    }
}
