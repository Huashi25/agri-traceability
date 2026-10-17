package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.OriginInfo;
import com.agri.traceability.service.OriginInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace/origin")
public class OriginInfoController {

    @Autowired
    private OriginInfoService originInfoService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long batchId) {
        QueryWrapper<OriginInfo> wrapper = new QueryWrapper<>();
        if (batchId != null) {
            wrapper.eq("batch_id", batchId);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(originInfoService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<OriginInfo> getById(@PathVariable Long id) {
        return Result.success(originInfoService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> save(@RequestBody OriginInfo origin) {
        originInfoService.saveOrUpdate(origin);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> update(@RequestBody OriginInfo origin) {
        originInfoService.updateById(origin);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> delete(@PathVariable Long id) {
        originInfoService.removeById(id);
        return Result.success();
    }
}
