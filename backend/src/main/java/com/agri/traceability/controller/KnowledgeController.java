package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.entity.KnowledgeArticle;
import com.agri.traceability.service.KnowledgeArticleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/list")
    public Result<List<KnowledgeArticle>> list(@RequestParam(required = false) String category) {
        QueryWrapper<KnowledgeArticle> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByDesc("sort", "create_time");
        // List view: exclude content field for performance
        wrapper.select("id", "title", "summary", "cover_image", "category", "tags", "view_count", "sort", "create_time");
        return Result.success(knowledgeArticleService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeArticle> getById(@PathVariable Long id) {
        KnowledgeArticle article = knowledgeArticleService.getById(id);
        if (article != null) {
            knowledgeArticleService.update(new UpdateWrapper<KnowledgeArticle>()
                    .eq("id", id).setSql("view_count = view_count + 1"));
            article.setViewCount(article.getViewCount() + 1);
        }
        return Result.success(article);
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        QueryWrapper<KnowledgeArticle> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).select("DISTINCT category");
        List<KnowledgeArticle> articles = knowledgeArticleService.list(wrapper);
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("SELECTION", "选购指南");
        labelMap.put("NUTRITION", "营养价值");
        labelMap.put("STORAGE", "储存保鲜");
        labelMap.put("USAGE", "食用方法");
        labelMap.put("GENERAL", "农业常识");

        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeArticle a : articles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", a.getCategory());
            item.put("label", labelMap.getOrDefault(a.getCategory(), a.getCategory()));
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/admin/list")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<List<KnowledgeArticle>> adminList() {
        QueryWrapper<KnowledgeArticle> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("sort", "create_time");
        return Result.success(knowledgeArticleService.list(wrapper));
    }

    @PostMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> save(@RequestBody KnowledgeArticle article) {
        knowledgeArticleService.saveOrUpdate(article);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> update(@RequestBody KnowledgeArticle article) {
        knowledgeArticleService.updateById(article);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeArticleService.removeById(id);
        return Result.success();
    }
}
