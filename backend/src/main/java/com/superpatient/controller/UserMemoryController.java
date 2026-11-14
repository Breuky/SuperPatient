package com.superpatient.controller;

import com.superpatient.dto.UserMemoryVO;
import com.superpatient.pojo.Result;
import com.superpatient.service.UserMemoryService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "长期记忆")
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class UserMemoryController {

    private final UserMemoryService service;

    @Operation(summary = "我的记忆条目")
    @GetMapping
    public Result list() {
        List<UserMemoryVO> list = service.list(CurrentHolder.getCurrentId());
        return Result.success(list);
    }

    @Operation(summary = "记住一条偏好或自述事实")
    @PostMapping
    public Result save(@RequestBody Map<String, String> body) {
        service.save(CurrentHolder.getCurrentId(), body.get("kind"),
                body.get("key"), body.get("value"));
        return Result.success(null);
    }

    @Operation(summary = "删除一条记忆")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(CurrentHolder.getCurrentId(), id);
        return Result.success(null);
    }

    @Operation(summary = "清空全部记忆")
    @DeleteMapping
    public Result clear() {
        service.clear(CurrentHolder.getCurrentId());
        return Result.success(null);
    }
}
