package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.entity.Product;
import com.agri.traceability.entity.ProductSeasonal;
import com.agri.traceability.service.ProductSeasonalService;
import com.agri.traceability.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/seasonal")
public class SeasonalController {

    @Autowired
    private ProductSeasonalService productSeasonalService;
    @Autowired
    private ProductService productService;

    @GetMapping("/current")
    public Result<List<Map<String, Object>>> current() {
        int month = LocalDate.now().getMonthValue();
        return getByMonth(month);
    }

    @GetMapping("/month/{month}")
    public Result<List<Map<String, Object>>> getByMonth(@PathVariable int month) {
        QueryWrapper<ProductSeasonal> wrapper = new QueryWrapper<>();
        wrapper.eq("month", month).orderByDesc("is_peak");
        List<ProductSeasonal> seasonals = productSeasonalService.list(wrapper);

        if (seasonals.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> productIds = new ArrayList<>();
        Map<Long, ProductSeasonal> seasonalMap = new HashMap<>();
        for (ProductSeasonal ps : seasonals) {
            productIds.add(ps.getProductId());
            seasonalMap.put(ps.getProductId(), ps);
        }

        List<Product> products = productService.listByIds(productIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product", p);
            ProductSeasonal ps = seasonalMap.get(p.getId());
            item.put("isPeak", ps.getIsPeak());
            item.put("seasonalDesc", ps.getDescription());
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/product/{productId}")
    public Result<List<ProductSeasonal>> byProduct(@PathVariable Long productId) {
        QueryWrapper<ProductSeasonal> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId).orderByAsc("month");
        return Result.success(productSeasonalService.list(wrapper));
    }

    @GetMapping("/admin/list")
    @RequireRole({"ADMIN"})
    public Result<List<ProductSeasonal>> adminList() {
        return Result.success(productSeasonalService.list(new QueryWrapper<ProductSeasonal>().orderByAsc("month")));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody ProductSeasonal seasonal) {
        productSeasonalService.saveOrUpdate(seasonal);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        productSeasonalService.removeById(id);
        return Result.success();
    }
}
