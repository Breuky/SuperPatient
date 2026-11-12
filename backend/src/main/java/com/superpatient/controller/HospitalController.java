package com.superpatient.controller;

import com.superpatient.pojo.Result;
import com.superpatient.dto.HospitalVO;
import com.superpatient.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "医院白名单")
@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @Operation(summary = "按城市与科室查医院",
            description = "只返回白名单内数据；verified=false 表示资质尚未人工核对")
    @GetMapping
    public Result list(@RequestParam(required = false) String city,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) Integer limit) {
        List<HospitalVO> list = hospitalService.find(city, department, limit);
        return Result.success(list);
    }
}