package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.entity.Product;
import com.agri.traceability.service.OrderInfoService;
import com.agri.traceability.service.OrderItemService;
import com.agri.traceability.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端订单控制器
 * 提供管理员和商家的订单管理接口
 *
 * 权限模型（2026-10-19 补）：
 *   ADMIN    —— 全平台订单可见、可操作；
 *   MERCHANT —— 只能看到"订单明细中包含自己名下商品"的订单，
 *               商品归属由 product.merchant_id 决定。
 *   之所以按"订单里有没有我的商品"来判断，是因为当前数据模型没有订单级的
 *   商家字段（order_info 只有 buyer 的 user_id）。要做到真正的"按商家拆单"，
 *   需要在下单时按商家拆分订单，属于另一处业务改造，此处只做可见性隔离，
 *   不改动下单流程。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/order")
@RequireRole({"ADMIN", "MERCHANT"})
public class AdminOrderController {

    @Autowired
    private OrderInfoService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ProductService productService;

    /**
     * 订单列表。
     * 管理员返回全部订单；商家只返回与自己商品相关的订单。
     *
     * @param status  可选的状态过滤
     * @param request 用于读取当前登录身份
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer status, HttpServletRequest request) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (status != null) wrapper.eq("status", status);

        // visibleOrderIds 返回 null 表示"不限制"（管理员）
        List<Long> visibleOrderIds = visibleOrderIds(request);
        if (visibleOrderIds != null) {
            if (visibleOrderIds.isEmpty()) {
                // 商家名下没有任何商品 → 没有任何订单可见，直接返回空列表
                return Result.success(Collections.emptyList());
            }
            wrapper.in("id", visibleOrderIds);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(orderService.list(wrapper));
    }

    /**
     * 订单详情。
     * 商家查看不属于自己的订单时返回无权限，而不是返回 404，
     * 避免商家通过遍历 ID 探测订单是否存在。
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id, HttpServletRequest request) {
        if (!canAccessOrder(id, request)) {
            log.warn("越权查看订单详情被拦截: orderId={}, currentUserId={}, role={}",
                    id, UserContext.currentUserId(request), UserContext.currentRole(request));
            return Result.error("无权查看该订单");
        }
        return orderService.getOrderDetail(id);
    }

    /**
     * 订单明细列表。同样受归属限制。
     */
    @GetMapping("/items/{orderId}")
    public Result getItems(@PathVariable Long orderId, HttpServletRequest request) {
        if (!canAccessOrder(orderId, request)) {
            log.warn("越权查看订单明细被拦截: orderId={}, currentUserId={}, role={}",
                    orderId, UserContext.currentUserId(request), UserContext.currentRole(request));
            return Result.error("无权查看该订单");
        }
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return Result.success(orderItemService.list(wrapper));
    }

    /**
     * 通用订单更新。
     *
     * 收紧为仅管理员：这个接口可以改任意字段（含 total_amount、收货信息），
     * 商家侧真正需要的状态流转已由 {@link #ship(Long, HttpServletRequest)} 和
     * {@link #cancel(Long, HttpServletRequest)} 覆盖，前端也从未调用过本接口。
     * 保留给商家会形成"商家可篡改任意订单金额"的口子。
     */
    @PutMapping
    @RequireRole({"ADMIN"})
    public Result<Void> update(@RequestBody OrderInfo order) {
        orderService.updateById(order);
        return Result.success();
    }

    /**
     * 商家/管理员发货
     */
    @PutMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id, HttpServletRequest request) {
        if (!canAccessOrder(id, request)) {
            log.warn("越权发货被拦截: orderId={}, currentUserId={}, role={}",
                    id, UserContext.currentUserId(request), UserContext.currentRole(request));
            return Result.error("无权操作该订单");
        }
        return orderService.shipOrder(id);
    }

    /**
     * 待发货工作台数据（商家补位功能之二）。
     *
     * 只返回"已付款待发货"（status=1）的订单，并顺带把订单明细一并查出，
     * 工作台一次渲染完，不用每行再发一次明细请求。
     * 商家只看到含自己商品的待发货订单。
     */
    @GetMapping("/pending-ship")
    public Result pendingShip(HttpServletRequest request) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        List<Long> visibleOrderIds = visibleOrderIds(request);
        if (visibleOrderIds != null) {
            if (visibleOrderIds.isEmpty()) {
                return Result.success(Collections.emptyList());
            }
            wrapper.in("id", visibleOrderIds);
        }
        // 先付款的先发，避免老订单被压单
        wrapper.orderByAsc("pay_time");

        List<OrderInfo> orders = orderService.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        if (!orders.isEmpty()) {
            // 一次性把明细查出来，避免逐单查询（N+1）
            List<Long> orderIds = orders.stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());
            QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.in("order_id", orderIds);
            Map<Long, List<OrderItem>> itemMap = orderItemService.list(itemWrapper).stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId));

            for (OrderInfo order : orders) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("order", order);
                row.put("items", itemMap.getOrDefault(order.getId(), Collections.emptyList()));
                result.add(row);
            }
        }
        return Result.success(result);
    }

    /**
     * 批量发货。
     *
     * 逐单调用 shipOrder，单笔失败不影响其他订单，最后把失败原因汇总返回，
     * 避免"一键发 20 单，中途一单异常就全回滚"的情况。
     * 每一单都要单独过归属校验，防止用批量接口绕过单条接口的越权检查。
     *
     * @param body { "ids": [订单ID, ...] }
     */
    @PutMapping("/batch-ship")
    public Result batchShip(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        List<Long> ids = body == null ? null : body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要发货的订单");
        }

        int success = 0;
        List<String> failed = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (!canAccessOrder(id, request)) {
                log.warn("批量发货越权被拦截: orderId={}, currentUserId={}, role={}",
                        id, UserContext.currentUserId(request), UserContext.currentRole(request));
                failed.add("订单 " + id + "：无权操作");
                continue;
            }
            Result<Void> single = orderService.shipOrder(id);
            // 200 视为成功，其余把服务层给的提示原样带回给前端
            if (single.getCode() == 200) {
                success++;
            } else {
                failed.add("订单 " + id + "：" + single.getMessage());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("failed", failed);
        log.info("批量发货完成: 请求={}, 成功={}, 失败={}", ids.size(), success, failed.size());
        return Result.success(data);
    }

    /**
     * 商家/管理员取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        if (!canAccessOrder(id, request)) {
            log.warn("越权取消订单被拦截: orderId={}, currentUserId={}, role={}",
                    id, UserContext.currentUserId(request), UserContext.currentRole(request));
            return Result.error("无权操作该订单");
        }
        return orderService.cancelOrder(id);
    }

    /**
     * 订单统计。商家只统计自己的订单，避免通过统计数字推断全平台经营情况。
     */
    @GetMapping("/stats")
    public Result stats(HttpServletRequest request) {
        List<Long> visibleOrderIds = visibleOrderIds(request);

        List<OrderInfo> allOrders;
        if (visibleOrderIds == null) {
            // 管理员：全量
            allOrders = orderService.list();
        } else if (visibleOrderIds.isEmpty()) {
            // 商家且名下无商品
            allOrders = Collections.emptyList();
        } else {
            QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
            wrapper.in("id", visibleOrderIds);
            allOrders = orderService.list(wrapper);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allOrders.size());
        stats.put("pending", allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count());
        stats.put("paid", allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count());
        stats.put("shipped", allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count());
        stats.put("completed", allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count());
        stats.put("cancelled", allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count());
        stats.put("totalAmount", allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 4 && o.getTotalAmount() != null)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum());
        return Result.success(stats);
    }

    // ==================== 归属判定（私有工具方法） ====================

    /**
     * 计算当前用户可见的订单 ID 集合。
     *
     * @return null 表示不做限制（管理员）；空集合表示一条都不可见（商家无商品）
     */
    private List<Long> visibleOrderIds(HttpServletRequest request) {
        if (UserContext.isAdmin(request)) {
            return null;
        }
        Long merchantId = UserContext.currentUserId(request);
        if (merchantId == null) {
            // 理论上 @RequireRole 已挡住未登录，这里做兜底：宁可见不到，也不越权
            return Collections.emptyList();
        }
        List<Long> productIds = merchantProductIds(merchantId);
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 只查 order_id 一列，减少数据传输
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.select("order_id").in("product_id", productIds);
        return orderItemService.list(itemWrapper).stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断当前用户是否可以访问指定订单。
     * 管理员直接放行；商家需要"订单明细里至少有一个商品归自己"。
     */
    private boolean canAccessOrder(Long orderId, HttpServletRequest request) {
        if (UserContext.isAdmin(request)) {
            return true;
        }
        Long merchantId = UserContext.currentUserId(request);
        if (merchantId == null || orderId == null) {
            return false;
        }
        // 先取该订单涉及的商品，再看其中有没有归属当前商家的
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.select("product_id").eq("order_id", orderId);
        List<Long> productIds = orderItemService.list(itemWrapper).stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return false;
        }
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("merchant_id", merchantId).in("id", productIds);
        return productService.count(productWrapper) > 0;
    }

    /**
     * 查询某商家名下的商品 ID 列表。
     */
    private List<Long> merchantProductIds(Long merchantId) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.select("id").eq("merchant_id", merchantId);
        return productService.list(wrapper).stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
