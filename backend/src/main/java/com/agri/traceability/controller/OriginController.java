package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Farmer;
import com.agri.traceability.service.FarmerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/origin")
public class OriginController {

    @Autowired
    private FarmerService farmerService;

    @GetMapping("/map")
    public Result<List<Farmer>> mapData() {
        QueryWrapper<Farmer> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .isNotNull("longitude")
               .isNotNull("latitude");
        return Result.success(farmerService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Farmer> getById(@PathVariable Long id) {
        return Result.success(farmerService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<Farmer>> list() {
        QueryWrapper<Farmer> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("create_time");
        return Result.success(farmerService.list(wrapper));
    }
}
