package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Product;
import com.agri.traceability.entity.UserFavorite;
import com.agri.traceability.service.ProductService;
import com.agri.traceability.service.UserFavoriteService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorite")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService favoriteService;
    @Autowired
    private ProductService productService;

    @PostMapping("/toggle/{productId}")
    public Result<Boolean> toggle(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        UserFavorite existing = favoriteService.getOne(wrapper);
        if (existing != null) {
            favoriteService.removeById(existing.getId());
            return Result.success(false);
        } else {
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            favoriteService.save(fav);
            return Result.success(true);
        }
    }

    @GetMapping("/list")
    public Result<List<Product>> list(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<UserFavorite> favorites = favoriteService.list(wrapper);
        if (favorites.isEmpty()) return Result.success(new ArrayList<>());
        List<Long> productIds = favorites.stream().map(UserFavorite::getProductId).collect(Collectors.toList());
        List<Product> products = productService.listByIds(productIds);
        return Result.success(products);
    }

    @GetMapping("/check/{productId}")
    public Result<Boolean> check(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        return Result.success(favoriteService.count(wrapper) > 0);
    }

    @GetMapping("/ids")
    public Result<List<Long>> favoritedIds(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserFavorite> favorites = favoriteService.list(wrapper);
        return Result.success(favorites.stream().map(UserFavorite::getProductId).collect(Collectors.toList()));
    }
}
