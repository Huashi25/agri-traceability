package com.agri.traceability.controller;

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
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stats")
public class PublicStatsController {

    @Autowired
    private ProductService productService;
    @Autowired
    private FarmerService farmerService;
    @Autowired
    private TraceBatchService traceBatchService;
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("productCount", productService.count());
        stats.put("farmerCount", farmerService.count());
        stats.put("traceCount", traceBatchService.count());
        stats.put("orderCount", orderInfoService.count());
        return Result.success(stats);
    }

    @GetMapping("/hot-products")
    public Result hotProducts() {
        List<OrderInfo> allOrders = orderInfoService.list();
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
            for (OrderItem item : allItems) {
                productSales.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        // 批量查询Top5商品信息
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
        return Result.success(result);
    }
}
