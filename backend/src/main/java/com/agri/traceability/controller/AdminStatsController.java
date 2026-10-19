package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.entity.Product;
import com.agri.traceability.entity.TraceBatch;
import com.agri.traceability.mapper.OrderItemMapper;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计控制器。
 *
 * 权限口径（2026-10-19 收口）：
 *   原实现对任何登录的管理端账号都返回全平台数据 —— 商家能看到全平台营业额、
 *   用户总数、农户总数，农户能看到全平台订单与营收，属于信息泄露。
 *   现按角色分开统计：
 *     ADMIN    —— 全平台口径（保持原样，不做任何削减）；
 *     MERCHANT —— 只统计自己名下商品、以及含自己商品的订单，
 *                 营收按"自己商品在有效订单中的小计"累加，
 *                 而不是整单金额（一单可能同时含其他商家的商品）；
 *     FARMER   —— 只统计自己绑定农户名下的溯源批次及其关联商品，
 *                 交易类指标（订单/营收/用户）一律为 0，因为农户不参与销售。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stats")
@RequireRole({"ADMIN", "MERCHANT", "FARMER"})
public class AdminStatsController {

    @Autowired
    private ProductService productService;
    @Autowired
    private OrderInfoService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private FarmerService farmerService;
    @Autowired
    private ProductReviewService reviewService;
    @Autowired
    private TraceBatchService traceBatchService;
    @Autowired
    private SysUserService userService;
    @Autowired
    private KnowledgeArticleService knowledgeArticleService;
    @Autowired
    private RecipeService recipeService;
    @Autowired
    private FaqService faqService;
    @Autowired
    private ProductSeasonalService productSeasonalService;
    @Autowired
    private AnnouncementService announcementService;

    /**
     * 概览数据。按当前角色返回各自口径，前端字段名完全一致，无需改渲染逻辑。
     */
    @GetMapping("/overview")
    public Result overview(HttpServletRequest request) {
        String role = UserContext.currentRole(request);
        if (UserContext.ROLE_MERCHANT.equals(role)) {
            return Result.success(merchantOverview(UserContext.currentUserId(request)));
        }
        if (UserContext.ROLE_FARMER.equals(role)) {
            return Result.success(farmerOverview(request));
        }
        // 管理员（以及任何未匹配角色）走全平台口径
        return Result.success(adminOverview());
    }

    /**
     * 热销商品 Top5。商家只统计自己商品的销量；农户不参与销售，返回空列表。
     */
    @GetMapping("/hot-products")
    public Result hotProducts(HttpServletRequest request) {
        String role = UserContext.currentRole(request);

        // 农户不卖货，热销榜对它没有业务含义，直接返回空
        if (UserContext.ROLE_FARMER.equals(role)) {
            return Result.success(Collections.emptyList());
        }

        // 商家的可见商品范围；管理员为 null（不限制）
        List<Long> scopeProductIds = null;
        if (UserContext.ROLE_MERCHANT.equals(role)) {
            scopeProductIds = merchantProductIds(UserContext.currentUserId(request));
            if (scopeProductIds.isEmpty()) {
                return Result.success(Collections.emptyList());
            }
        }

        return Result.success(buildHotProducts(scopeProductIds));
    }

    // ==================== 各角色口径实现 ====================

    /**
     * 管理员口径：全平台统计，与原实现完全一致。
     */
    private Map<String, Object> adminOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("productCount", productService.count());
        data.put("orderCount", orderService.count());
        data.put("farmerCount", farmerService.count());
        data.put("reviewCount", reviewService.count());
        data.put("traceCount", traceBatchService.count());
        data.put("userCount", userService.count());
        data.put("knowledgeCount", knowledgeArticleService.count());
        data.put("recipeCount", recipeService.count());
        data.put("faqCount", faqService.count());
        data.put("seasonalCount", productSeasonalService.count());
        data.put("announcementCount", announcementService.count());

        List<OrderInfo> orders = orderService.list();
        data.put("totalRevenue", round2(sumOrderAmount(orders)));
        data.put("orderStatus", countOrderStatus(orders));
        return data;
    }

    /**
     * 商家口径：只统计自己名下的商品与相关订单。
     *
     * 平台级指标（用户数/农户数/内容数）一律给 0：
     * 这些不是商家经营数据，给真实值就是泄露。
     */
    private Map<String, Object> merchantOverview(Long merchantId) {
        Map<String, Object> data = new HashMap<>();

        List<Long> myProductIds = merchantProductIds(merchantId);
        data.put("productCount", myProductIds.size());

        // 平台级指标：商家无权查看
        data.put("farmerCount", 0);
        data.put("userCount", 0);
        data.put("knowledgeCount", 0);
        data.put("recipeCount", 0);
        data.put("faqCount", 0);
        data.put("seasonalCount", 0);
        data.put("announcementCount", 0);

        if (myProductIds.isEmpty()) {
            // 名下没有商品：其余指标全部为 0，避免再发无意义的查询
            data.put("orderCount", 0);
            data.put("reviewCount", 0);
            data.put("traceCount", 0);
            data.put("totalRevenue", 0.0);
            data.put("orderStatus", countOrderStatus(Collections.emptyList()));
            return data;
        }

        // 评价数与批次数直接按商品范围统计
        QueryWrapper<com.agri.traceability.entity.ProductReview> reviewWrapper = new QueryWrapper<>();
        reviewWrapper.in("product_id", myProductIds);
        data.put("reviewCount", reviewService.count(reviewWrapper));

        QueryWrapper<TraceBatch> traceWrapper = new QueryWrapper<>();
        traceWrapper.in("product_id", myProductIds);
        data.put("traceCount", traceBatchService.count(traceWrapper));

        // 订单：只算"明细里含我商品"的订单
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.in("product_id", myProductIds);
        List<OrderItem> myItems = orderItemService.list(itemWrapper);

        Set<Long> orderIds = myItems.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<OrderInfo> myOrders = orderIds.isEmpty()
                ? Collections.emptyList()
                : orderService.listByIds(orderIds);

        data.put("orderCount", myOrders.size());
        data.put("orderStatus", countOrderStatus(myOrders));

        // 营收 = 自己商品在"未取消订单"中的小计合计
        Set<Long> validOrderIds = myOrders.stream()
                .filter(o -> o.getStatus() == null || o.getStatus() != 4)
                .map(OrderInfo::getId)
                .collect(Collectors.toSet());
        double revenue = myItems.stream()
                .filter(i -> i.getOrderId() != null && validOrderIds.contains(i.getOrderId()))
                .mapToDouble(i -> i.getSubtotal() == null ? 0 : i.getSubtotal().doubleValue())
                .sum();
        data.put("totalRevenue", round2(revenue));

        return data;
    }

    /**
     * 农户口径：只看自己绑定农户名下的批次，以及这些批次关联的商品。
     *
     * 农户不参与销售，订单/营收/用户类指标全部给 0。
     */
    private Map<String, Object> farmerOverview(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();

        Long farmerId = userService.getFarmerId(UserContext.currentUserId(request));
        if (farmerId == null) {
            // 账号未绑定农户：所有指标归零，不泄露任何平台数据
            log.warn("农户账号未绑定 farmer_id: userId={}", UserContext.currentUserId(request));
            data.put("productCount", 0);
            data.put("orderCount", 0);
            data.put("farmerCount", 0);
            data.put("reviewCount", 0);
            data.put("traceCount", 0);
            data.put("userCount", 0);
            data.put("knowledgeCount", 0);
            data.put("recipeCount", 0);
            data.put("faqCount", 0);
            data.put("seasonalCount", 0);
            data.put("announcementCount", 0);
            data.put("totalRevenue", 0.0);
            data.put("orderStatus", countOrderStatus(Collections.emptyList()));
            return data;
        }

        QueryWrapper<TraceBatch> traceWrapper = new QueryWrapper<>();
        traceWrapper.eq("farmer_id", farmerId);
        List<TraceBatch> myBatches = traceBatchService.list(traceWrapper);
        data.put("traceCount", myBatches.size());

        // 我的批次覆盖到哪些商品（去重）
        long productCount = myBatches.stream()
                .map(TraceBatch::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        data.put("productCount", productCount);

        // 农户侧的其余指标
        data.put("farmerCount", 0);
        data.put("orderCount", 0);
        data.put("reviewCount", 0);
        data.put("userCount", 0);
        data.put("knowledgeCount", 0);
        data.put("recipeCount", 0);
        data.put("faqCount", 0);
        data.put("seasonalCount", 0);
        data.put("announcementCount", 0);
        data.put("totalRevenue", 0.0);
        data.put("orderStatus", countOrderStatus(Collections.emptyList()));
        return data;
    }

    // ==================== 公共工具方法 ====================

    /**
     * 组装热销商品 Top5。
     *
     * @param scopeProductIds 限定统计的商品范围；null 表示不限制（管理员）
     */
    private List<Map<String, Object>> buildHotProducts(List<Long> scopeProductIds) {
        // 只统计非取消订单
        List<OrderInfo> allOrders = orderService.list();
        List<Long> validOrderIds = allOrders.stream()
                .filter(o -> o.getStatus() == null || o.getStatus() != 4)
                .map(OrderInfo::getId)
                .collect(Collectors.toList());

        Map<Long, Integer> productSales = new HashMap<>();
        if (!validOrderIds.isEmpty()) {
            QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.in("order_id", validOrderIds);
            // 商家只看自己商品 → 直接在 SQL 层加商品范围，减少无谓数据传输
            if (scopeProductIds != null) {
                itemWrapper.in("product_id", scopeProductIds);
            }
            List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);
            for (OrderItem item : allItems) {
                if (item.getProductId() != null && item.getQuantity() != null) {
                    productSales.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                }
            }
        }
        // 兜底：如果订单明细里有不属于本商家的脏数据，这里再过滤一次
        if (scopeProductIds != null) {
            Set<Long> allowed = new HashSet<>(scopeProductIds);
            productSales.keySet().retainAll(allowed);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<Map.Entry<Long, Integer>> top5 = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (!top5.isEmpty()) {
            List<Long> productIds = top5.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            List<Product> products = productMapper.selectBatchIds(productIds);
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (Map.Entry<Long, Integer> entry : top5) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", entry.getKey());
                item.put("salesCount", entry.getValue());
                Product product = productMap.get(entry.getKey());
                if (product != null) {
                    item.put("productName", product.getName());
                    item.put("price", product.getPrice());
                    item.put("image", product.getImage());
                    item.put("origin", product.getOrigin());
                }
                result.add(item);
            }
        }
        log.debug("热销商品统计: top5={}, 范围={}", result.size(),
                scopeProductIds == null ? "全平台" : ("商品数" + scopeProductIds.size()));
        return result;
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

    /**
     * 订单状态分布。取消订单也算一种状态，保留在分布里便于前端展示。
     */
    private Map<String, Long> countOrderStatus(List<OrderInfo> orders) {
        Map<String, Long> orderStatus = new HashMap<>();
        orderStatus.put("pending", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count());
        orderStatus.put("paid", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
        orderStatus.put("shipped", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
        orderStatus.put("completed", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count());
        orderStatus.put("cancelled", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
        return orderStatus;
    }

    /**
     * 订单金额合计（排除已取消订单）。
     */
    private double sumOrderAmount(List<OrderInfo> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 4)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();
    }

    /**
     * 保留两位小数，避免浮点误差直接暴露到前端。
     */
    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
