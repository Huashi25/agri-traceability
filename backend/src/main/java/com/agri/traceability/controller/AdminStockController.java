package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.Product;
import com.agri.traceability.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存预警控制器（商家补位功能之一）。
 *
 * 背景：商家后台原有"商品管理"只能逐条翻页看库存，15 个商品还行，
 * 商品多了就发现不了"哪个快断货"。补一个按阈值自动筛出低库存商品的接口。
 *
 * 权限：ADMIN + MERCHANT。
 *   商家只统计自己名下商品；管理员可看全平台。
 *   农户不参与销售，没有商品库存，不在放行范围内。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stock")
@RequireRole({"ADMIN", "MERCHANT"})
public class AdminStockController {

    /** 默认预警阈值：库存 ≤ 该值即视为需要补货 */
    private static final int DEFAULT_THRESHOLD = 10;

    @Autowired
    private ProductService productService;

    /**
     * 低库存商品列表。
     *
     * @param threshold 预警阈值，不传默认 10；负数按 0 处理
     * @param request   用于读取当前商家身份
     * @return 阈值、断货数、偏低数、明细列表（按库存升序）
     */
    @GetMapping("/warning")
    public Result warning(@RequestParam(required = false) Integer threshold,
                          HttpServletRequest request) {
        int limit = (threshold == null || threshold < 0) ? DEFAULT_THRESHOLD : threshold;

        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        // 只看上架商品：下架商品的库存不影响销售，提示它只会造成噪音
        wrapper.eq("status", 1);
        wrapper.le("stock", limit);
        if (!UserContext.isAdmin(request)) {
            // 商家只看自己的商品，避免看到竞争对手的库存
            wrapper.eq("merchant_id", UserContext.currentUserId(request));
        }
        wrapper.orderByAsc("stock");

        List<Product> products = productService.list(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        int outCount = 0;
        int lowCount = 0;

        for (Product p : products) {
            int stock = p.getStock() == null ? 0 : p.getStock();
            // 断货与偏低分开统计：断货要立刻处理，偏低还有缓冲
            String level = stock <= 0 ? "OUT" : "LOW";
            if ("OUT".equals(level)) {
                outCount++;
            } else {
                lowCount++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("image", p.getImage());
            item.put("stock", stock);
            item.put("price", p.getPrice());
            item.put("origin", p.getOrigin());
            item.put("salesCount", p.getSalesCount() == null ? 0 : p.getSalesCount());
            item.put("merchantId", p.getMerchantId());
            item.put("level", level);
            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("threshold", limit);
        data.put("outCount", outCount);
        data.put("lowCount", lowCount);
        data.put("list", list);

        log.debug("库存预警查询: role={}, threshold={}, 命中={}",
                UserContext.currentRole(request), limit, list.size());
        return Result.success(data);
    }
}
