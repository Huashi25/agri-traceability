package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.service.OrderInfoService;
import com.agri.traceability.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端订单控制器
 * 提供管理员和商家的订单管理接口
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

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer status) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("create_time");
        return Result.success(orderService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @GetMapping("/items/{orderId}")
    public Result getItems(@PathVariable Long orderId) {
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return Result.success(orderItemService.list(wrapper));
    }

    @PutMapping
    public Result<Void> update(@RequestBody OrderInfo order) {
        orderService.updateById(order);
        return Result.success();
    }

    /**
     * 商家/管理员发货
     */
    @PutMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id) {
        return orderService.shipOrder(id);
    }

    /**
     * 商家/管理员取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping("/stats")
    public Result stats() {
        List<OrderInfo> allOrders = orderService.list();
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
}
