package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.TraceBatch;
import com.agri.traceability.service.TraceBatchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace/batch")
public class TraceBatchController {

    @Autowired
    private TraceBatchService traceBatchService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long productId) {
        QueryWrapper<TraceBatch> wrapper = new QueryWrapper<>();
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(traceBatchService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<TraceBatch> getById(@PathVariable Long id) {
        return Result.success(traceBatchService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "FARMER"})
    public Result<TraceBatch> create(@RequestBody TraceBatch batch) {
        if (batch.getBatchNo() == null || batch.getBatchNo().isEmpty()) {
            batch.setBatchNo(traceBatchService.generateBatchNo());
        }
        if (batch.getTraceCode() == null || batch.getTraceCode().isEmpty()) {
            batch.setTraceCode(traceBatchService.generateTraceCode(batch.getProductId()));
        }
        if (batch.getStatus() == null) {
            batch.setStatus(0);
        }
        traceBatchService.save(batch);
        return Result.success(batch);
    }

    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT", "FARMER"})
    public Result<Void> update(@RequestBody TraceBatch batch) {
        traceBatchService.updateById(batch);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        traceBatchService.removeById(id);
        return Result.success();
    }
}
