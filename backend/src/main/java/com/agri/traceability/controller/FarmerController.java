package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Farmer;
import com.agri.traceability.service.FarmerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmer")
public class FarmerController {
    @Autowired
    private FarmerService farmerService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String name) {
        QueryWrapper<Farmer> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) wrapper.like("name", name);
        wrapper.orderByDesc("create_time");
        return Result.success(farmerService.list(wrapper));
    }

    @PostMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> save(@RequestBody Farmer farmer) {
        farmerService.saveOrUpdate(farmer);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> update(@RequestBody Farmer farmer) {
        farmerService.updateById(farmer);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> delete(@PathVariable Long id) {
        farmerService.removeById(id);
        return Result.success();
    }
}
