package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.entity.Faq;
import com.agri.traceability.service.FaqService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;

    @GetMapping("/list")
    public Result<List<Faq>> list(@RequestParam(required = false) String category) {
        QueryWrapper<Faq> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByAsc("sort");
        return Result.success(faqService.list(wrapper));
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("TRACE", "溯源查询");
        labelMap.put("ORDER", "订单问题");
        labelMap.put("PAYMENT", "支付问题");
        labelMap.put("SHIPPING", "配送问题");
        labelMap.put("AFTER_SALE", "售后问题");
        labelMap.put("ACCOUNT", "账号问题");

        QueryWrapper<Faq> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).select("DISTINCT category");
        List<Faq> faqs = faqService.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Faq f : faqs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", f.getCategory());
            item.put("label", labelMap.getOrDefault(f.getCategory(), f.getCategory()));
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody Faq faq) {
        faqService.saveOrUpdate(faq);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN"})
    public Result<Void> update(@RequestBody Faq faq) {
        faqService.updateById(faq);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        faqService.removeById(id);
        return Result.success();
    }
}
