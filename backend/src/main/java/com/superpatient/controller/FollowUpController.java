package com.superpatient.controller;

import com.superpatient.dto.FollowUpVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.FollowUpService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "随访提醒", description = "基于用户自身复查间隔的待办，不构成医疗建议")
@RestController
@RequestMapping("/api/follow-ups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @Operation(summary = "待复查列表", description = "读取时重算一次，随后返回 pending 项")
    @GetMapping
    public Result list() {
        List<FollowUpVO> list = followUpService.list(CurrentHolder.getCurrentId());
        return Result.success(list);
    }

    @Operation(summary = "标记完成或忽略", description = "body: {\"status\":\"done\"} 或 dismissed")
    @PutMapping("/{id}")
    public Result updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean updated = followUpService.updateStatus(
                id, body.get("status"), CurrentHolder.getCurrentId());
        if (!updated) return Result.error("提醒不存在或状态非法");
        return Result.success(null);
    }
}
