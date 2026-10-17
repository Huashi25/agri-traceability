package com.agri.traceability.controller;

import com.agri.traceability.common.PageResult;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Product;
import com.agri.traceability.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String name,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer pageSize) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByDesc("create_time");
        if (page != null && pageSize != null) {
            IPage<Product> pageResult = productService.page(new Page<>(page, pageSize), wrapper);
            return Result.success(PageResult.of(pageResult));
        }
        return Result.success(productService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> save(@RequestBody Product product) {
        productService.saveOrUpdate(product);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> update(@RequestBody Product product) {
        productService.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.success();
    }
}
