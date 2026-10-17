package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.ProductCategory;
import com.agri.traceability.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    @GetMapping("/list")
    public Result list() {
        return Result.success(categoryService.list());
    }

    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody ProductCategory category) {
        categoryService.saveOrUpdate(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }
}
