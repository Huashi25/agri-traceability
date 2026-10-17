package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.LogisticsInfo;
import com.agri.traceability.service.LogisticsInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace/logistics")
public class LogisticsInfoController {

    @Autowired
    private LogisticsInfoService logisticsService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long batchId) {
        QueryWrapper<LogisticsInfo> wrapper = new QueryWrapper<>();
        if (batchId != null) {
            wrapper.eq("batch_id", batchId);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(logisticsService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<LogisticsInfo> getById(@PathVariable Long id) {
        return Result.success(logisticsService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> save(@RequestBody LogisticsInfo logistics) {
        logisticsService.saveOrUpdate(logistics);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> update(@RequestBody LogisticsInfo logistics) {
        logisticsService.updateById(logistics);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> delete(@PathVariable Long id) {
        logisticsService.removeById(id);
        return Result.success();
    }
}
