package com.superpatient.controller;

import com.superpatient.dto.CategoryCreateRequest;
import com.superpatient.dto.CategoryUpdateRequest;
import com.superpatient.dto.CategoryVO;
import com.superpatient.entity.ExamCategory;
import com.superpatient.pojo.Result;
import com.superpatient.service.CategoryService;
import com.superpatient.util.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "检查分类", description = "检查分类管理（含指标）")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表（含指标）", description = "仅返回启用状态的指标")
    @GetMapping
    public Result list() {
        List<CategoryVO> list = categoryService.listWithIndicators(CurrentHolder.getCurrentId());
        return Result.success(list);
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result create(@RequestBody CategoryCreateRequest request) {
        ExamCategory category = categoryService.create(request);
        return Result.success(category);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody CategoryUpdateRequest request) {
        boolean updated = categoryService.update(id, request);
        if (!updated) return Result.error("分类不存在");
        return Result.success(null);
    }

    @Operation(summary = "删除分类", description = "级联删除该分类下的所有指标和检查值")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        boolean deleted = categoryService.delete(id);
        if (!deleted) return Result.error("分类不存在");
        return Result.success(null);
    }
}
