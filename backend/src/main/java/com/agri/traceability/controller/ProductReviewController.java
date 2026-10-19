package com.agri.traceability.controller;

import com.agri.traceability.common.PageResult;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.*;
import com.agri.traceability.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/review")
public class ProductReviewController {
    @Autowired
    private ProductReviewService reviewService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private OrderInfoService orderService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long productId,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer pageSize,
                       @RequestParam(required = false) Boolean mine,
                       HttpServletRequest request) {
        QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
        if (productId != null) wrapper.eq("product_id", productId);
        wrapper.eq("status", 1);

        // 商家后台评价管理：只看自己名下商品的评价。
        // 消费者端（含游客）不传 mine，行为与改造前完全一致。
        if (Boolean.TRUE.equals(mine) && UserContext.ROLE_MERCHANT.equals(UserContext.currentRole(request))) {
            List<Long> myProductIds = merchantProductIds(UserContext.currentUserId(request));
            if (myProductIds.isEmpty()) {
                return Result.success(Collections.emptyList());
            }
            wrapper.in("product_id", myProductIds);
        }

        wrapper.orderByDesc("create_time");
        if (page != null && pageSize != null) {
            IPage<ProductReview> pageResult = reviewService.page(new Page<>(page, pageSize), wrapper);
            return Result.success(PageResult.of(pageResult));
        }
        return Result.success(reviewService.list(wrapper));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ProductReview review, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        String username = request.getAttribute("username").toString();

        if (review.getProductId() == null) {
            return Result.error("商品ID不能为空");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return Result.error("评分需在1-5之间");
        }
        if (review.getContent() == null || review.getContent().trim().isEmpty()) {
            return Result.error("评价内容不能为空");
        }

        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("product_id", review.getProductId());
        List<OrderItem> items = orderItemService.list(itemWrapper);
        if (items.isEmpty()) {
            return Result.error("您尚未购买过该商品，无法评价");
        }
        List<Long> orderIds = items.stream().map(OrderItem::getOrderId).collect(Collectors.toList());
        QueryWrapper<OrderInfo> orderWrapper = new QueryWrapper<>();
        orderWrapper.in("id", orderIds)
                .eq("user_id", userId)
                .eq("status", 3);
        long completedCount = orderService.count(orderWrapper);
        if (completedCount == 0) {
            return Result.error("请确认收货后再进行评价");
        }

        QueryWrapper<ProductReview> existWrapper = new QueryWrapper<>();
        existWrapper.eq("product_id", review.getProductId())
                .eq("user_id", userId);
        if (reviewService.count(existWrapper) > 0) {
            return Result.error("您已评价过该商品");
        }

        review.setUserId(userId);
        review.setUserName(username);
        review.setStatus(1);
        reviewService.save(review);
        recalculateRating(review.getProductId());
        return Result.success();
    }

    private void recalculateRating(Long productId) {
        QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId).eq("status", 1);
        List<ProductReview> reviews = reviewService.list(wrapper);
        Product product = productService.getById(productId);
        if (product == null) return;
        if (reviews.isEmpty()) {
            product.setRating(BigDecimal.ZERO);
            product.setReviewCount(0);
        } else {
            double avgRating = reviews.stream()
                .mapToInt(r -> r.getRating() != null ? r.getRating() : 5)
                .average()
                .orElse(0.0);
            product.setRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
            product.setReviewCount(reviews.size());
        }
        productService.updateById(product);
    }

    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> update(@RequestBody ProductReview review, HttpServletRequest request) {
        ProductReview exists = review.getId() == null ? null : reviewService.getById(review.getId());
        if (exists == null) {
            return Result.error("评价不存在");
        }
        // 商家只能处理自己商品的评价，否则可以随意隐藏/恢复他人的评价
        if (!UserContext.isAdmin(request) && !ownsProduct(request, exists.getProductId())) {
            log.warn("越权修改评价被拦截: reviewId={}, currentUserId={}, productId={}",
                    review.getId(), UserContext.currentUserId(request), exists.getProductId());
            return Result.error("无权操作该评价");
        }
        reviewService.updateById(review);
        // 用库里的 productId 重算，避免请求体不带 productId 时算错/空指针
        recalculateRating(exists.getProductId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        ProductReview review = reviewService.getById(id);
        if (review != null) {
            if (!UserContext.isAdmin(request) && !ownsProduct(request, review.getProductId())) {
                log.warn("越权删除评价被拦截: reviewId={}, currentUserId={}, productId={}",
                        id, UserContext.currentUserId(request), review.getProductId());
                return Result.error("无权操作该评价");
            }
            reviewService.removeById(id);
            recalculateRating(review.getProductId());
        }
        return Result.success();
    }

    // ==================== 归属判定（私有工具方法） ====================

    /**
     * 判断当前登录商家是否拥有指定商品。
     * 管理员不走这里（调用前已放行）。
     */
    private boolean ownsProduct(HttpServletRequest request, Long productId) {
        Long merchantId = UserContext.currentUserId(request);
        if (merchantId == null || productId == null) {
            return false;
        }
        Product product = productService.getById(productId);
        return product != null && merchantId.equals(product.getMerchantId());
    }

    /**
     * 查询某商家名下的商品 ID 列表。
     */
    private List<Long> merchantProductIds(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.select("id").eq("merchant_id", merchantId);
        return productService.list(wrapper).stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
