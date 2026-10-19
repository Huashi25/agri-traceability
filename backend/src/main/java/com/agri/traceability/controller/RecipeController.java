package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.entity.Product;
import com.agri.traceability.entity.Recipe;
import com.agri.traceability.entity.RecipeProduct;
import com.agri.traceability.service.ProductService;
import com.agri.traceability.service.RecipeProductService;
import com.agri.traceability.service.RecipeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/recipe")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;
    @Autowired
    private RecipeProductService recipeProductService;
    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result<List<Recipe>> list(@RequestParam(required = false) String category) {
        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByDesc("view_count", "create_time");
        // List view: exclude steps field for performance
        wrapper.select("id", "title", "description", "cover_image", "cook_time", "servings", "difficulty", "category", "view_count", "create_time");
        return Result.success(recipeService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Recipe recipe = recipeService.getById(id);
        if (recipe == null) return Result.success(null);

        recipeService.update(new UpdateWrapper<Recipe>()
                .eq("id", id).setSql("view_count = view_count + 1"));
        recipe.setViewCount(recipe.getViewCount() + 1);

        QueryWrapper<RecipeProduct> rpWrapper = new QueryWrapper<>();
        rpWrapper.eq("recipe_id", id);
        List<RecipeProduct> rpList = recipeProductService.list(rpWrapper);

        List<Map<String, Object>> ingredients = new ArrayList<>();
        if (!rpList.isEmpty()) {
            List<Long> productIds = new ArrayList<>();
            Map<Long, RecipeProduct> rpMap = new HashMap<>();
            for (RecipeProduct rp : rpList) {
                productIds.add(rp.getProductId());
                rpMap.put(rp.getProductId(), rp);
            }
            List<Product> products = productService.listByIds(productIds);
            for (Product p : products) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("product", p);
                RecipeProduct rp = rpMap.get(p.getId());
                item.put("amount", rp.getAmount());
                item.put("isMain", rp.getIsMainIngredient());
                ingredients.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recipe", recipe);
        result.put("ingredients", ingredients);
        return Result.success(result);
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("DESSERT", "甜品");
        labelMap.put("STAPLE", "主食");
        labelMap.put("SOUP", "汤品");
        labelMap.put("SIDE", "小菜");
        labelMap.put("JUICE", "果汁");

        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).select("DISTINCT category");
        List<Recipe> recipes = recipeService.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Recipe r : recipes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", r.getCategory());
            item.put("label", labelMap.getOrDefault(r.getCategory(), r.getCategory()));
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}/products")
 public Result<List<Map<String, Object>>> getProducts(@PathVariable Long id) {
 QueryWrapper<RecipeProduct> rpWrapper = new QueryWrapper<>();
 rpWrapper.eq("recipe_id", id);
 List<RecipeProduct> rpList = recipeProductService.list(rpWrapper);

 List<Map<String, Object>> result = new ArrayList<>();
 if (!rpList.isEmpty()) {
 List<Long> productIds = new ArrayList<>();
 Map<Long, RecipeProduct> rpMap = new HashMap<>();
 for (RecipeProduct rp : rpList) {
 productIds.add(rp.getProductId());
 rpMap.put(rp.getProductId(), rp);
 }
 List<Product> products = productService.listByIds(productIds);
 for (Product p : products) {
 Map<String, Object> item = new LinkedHashMap<>();
 item.put("productId", p.getId());
 item.put("productName", p.getName());
 item.put("productImage", p.getImage());
 item.put("productPrice", p.getPrice());
 RecipeProduct rp = rpMap.get(p.getId());
 item.put("amount", rp.getAmount());
 item.put("isMain", rp.getIsMainIngredient());
 result.add(item);
 }
 }
 return Result.success(result);
 }

 @PostMapping("/{id}/products")
 @RequireRole({"ADMIN"})
 public Result<Void> saveProducts(@PathVariable Long id, @RequestBody List<RecipeProduct> products) {
 recipeProductService.remove(new QueryWrapper<RecipeProduct>().eq("recipe_id", id));
 if (products != null && !products.isEmpty()) {
 for (RecipeProduct rp : products) {
 rp.setRecipeId(id);
 recipeProductService.save(rp);
 }
 }
 return Result.success();
 }

 @PostMapping
 @RequireRole({"ADMIN"})
 public Result<Long> save(@RequestBody Recipe recipe) {
 recipeService.saveOrUpdate(recipe);
 return Result.success(recipe.getId());
 }

    @PutMapping
    @RequireRole({"ADMIN"})
    public Result<Void> update(@RequestBody Recipe recipe) {
        recipeService.updateById(recipe);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        recipeService.removeById(id);
        recipeProductService.remove(new QueryWrapper<RecipeProduct>().eq("recipe_id", id));
        return Result.success();
    }
}
