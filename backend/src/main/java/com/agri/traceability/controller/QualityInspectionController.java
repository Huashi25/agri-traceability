package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.QualityInspection;
import com.agri.traceability.service.QualityInspectionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace/inspection")
public class QualityInspectionController {

    @Autowired
    private QualityInspectionService inspectionService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long batchId) {
        QueryWrapper<QualityInspection> wrapper = new QueryWrapper<>();
        if (batchId != null) {
            wrapper.eq("batch_id", batchId);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(inspectionService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<QualityInspection> getById(@PathVariable Long id) {
        return Result.success(inspectionService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> save(@RequestBody QualityInspection inspection) {
        inspectionService.saveOrUpdate(inspection);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> update(@RequestBody QualityInspection inspection) {
        inspectionService.updateById(inspection);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "FARMER"})
    public Result<Void> delete(@PathVariable Long id) {
        inspectionService.removeById(id);
        return Result.success();
    }
}
