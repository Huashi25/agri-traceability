package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.entity.Product;
import com.agri.traceability.mapper.OrderItemMapper;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/overview")
    public Result overview() {
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

        // 订单金额统计
        java.util.List<OrderInfo> orders = orderService.list();
        double totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 4)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();
        data.put("totalRevenue", Math.round(totalRevenue * 100) / 100.0);

        // 订单状态分布
        Map<String, Long> orderStatus = new HashMap<>();
        orderStatus.put("pending", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count());
        orderStatus.put("paid", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
        orderStatus.put("shipped", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
        orderStatus.put("completed", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count());
        orderStatus.put("cancelled", orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
        data.put("orderStatus", orderStatus);

        return Result.success(data);
    }

    @GetMapping("/hot-products")
    public Result hotProducts() {
        // 查询所有非取消订单
        List<OrderInfo> allOrders = orderService.list();
        List<Long> validOrderIds = allOrders.stream()
                .filter(o -> o.getStatus() == null || o.getStatus() != 4)
                .map(OrderInfo::getId)
                .collect(Collectors.toList());

        // 批量查询所有有效订单的明细（解决N+1查询问题）
        Map<Long, Integer> productSales = new HashMap<>();
        if (!validOrderIds.isEmpty()) {
            QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.in("order_id", validOrderIds);
            List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);
            // 内存聚合销量
            for (OrderItem item : allItems) {
                productSales.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        // 按销量排序取前5，批量查询商品信息
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map.Entry<Long, Integer>> top5 = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (!top5.isEmpty()) {
            List<Long> productIds = top5.stream().map(Map.Entry::getKey).collect(Collectors.toList());
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
        log.debug("热销商品统计: top5={}", result.size());
        return Result.success(result);
    }
}
