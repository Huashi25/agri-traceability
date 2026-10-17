package com.agri.traceability.controller;

import com.agri.traceability.common.CheckoutDTO;
import com.agri.traceability.common.OrderCreateDTO;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.service.OrderInfoService;
import com.agri.traceability.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * 负责接收前端请求，调用Service层处理业务逻辑，返回统一结果
 * 遵循三层架构原则：Controller层只做参数接收和结果返回，业务逻辑下沉至Service层
 */
@Slf4j
@Api(tags = "订单管理", description = "订单创建、支付、发货、收货全生命周期")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderInfoService orderService;
    @Autowired
    private OrderItemService orderItemService;

    @ApiOperation("查询用户订单列表")
    @GetMapping("/list")
    public Result list(@ApiParam(value = "用户ID", required = true) @RequestParam Long userId) {
        return orderService.listUserOrders(userId);
    }

    @ApiOperation("查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderInfo> getById(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @ApiOperation("查询订单明细列表")
    @GetMapping("/items/{orderId}")
    public Result<List<OrderItem>> getItems(@PathVariable Long orderId) {
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return Result.success(orderItemService.list(wrapper));
    }

    @ApiOperation("创建订单（立即购买）")
    @PostMapping
    public Result<OrderInfo> create(@RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(dto);
    }

    @ApiOperation("购物车结算")
    @PostMapping("/checkout")
    public Result<OrderInfo> checkout(@RequestBody CheckoutDTO dto) {
        return orderService.checkout(dto);
    }

    @ApiOperation("支付订单")
    @PutMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        return orderService.payOrder(id);
    }

    @ApiOperation("取消订单（恢复库存）")
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @ApiOperation("发货")
    @PutMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id) {
        return orderService.shipOrder(id);
    }

    @ApiOperation("确认收货")
    @PutMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id) {
        return orderService.confirmOrder(id);
    }

    @ApiOperation("更新订单信息")
    @PutMapping
    public Result<Void> update(@RequestBody OrderInfo order) {
        orderService.updateById(order);
        return Result.success();
    }
}
