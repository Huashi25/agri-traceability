package com.agri.traceability.controller;

import com.agri.traceability.common.CheckoutDTO;
import com.agri.traceability.common.OrderCreateDTO;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 订单控制器（买家侧）
 * 负责接收前端请求，调用Service层处理业务逻辑，返回统一结果
 * 遵循三层架构原则：Controller层只做参数接收和结果返回，业务逻辑下沉至Service层
 *
 * 安全约定（2026-10-19 加固）：本接口面向买家，"是谁的订单"一律以 JWT 中的用户身份为准，
 * 绝不采信前端传入的 userId / orderId 归属；管理员走 /api/admin/order 独立入口。
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
    public Result list(@ApiParam(value = "用户ID（仅管理员生效）") @RequestParam(required = false) Long userId,
                       HttpServletRequest request) {
        // 越权防护：普通用户只能看自己的订单，前端传的 userId 直接忽略
        Long targetUserId = UserContext.isAdmin(request)
                ? (userId != null ? userId : UserContext.currentUserId(request))
                : UserContext.currentUserId(request);
        if (targetUserId == null) {
            return Result.error("未登录或登录已过期");
        }
        return orderService.listUserOrders(targetUserId);
    }

    @ApiOperation("查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderInfo> getById(@PathVariable Long id, HttpServletRequest request) {
        String err = orderAccessError(id, request);
        if (err != null) {
            return Result.error(err);
        }
        return orderService.getOrderDetail(id);
    }

    @ApiOperation("查询订单明细列表")
    @GetMapping("/items/{orderId}")
    public Result<List<OrderItem>> getItems(@PathVariable Long orderId, HttpServletRequest request) {
        String err = orderAccessError(orderId, request);
        if (err != null) {
            return Result.error(err);
        }
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return Result.success(orderItemService.list(wrapper));
    }

    @ApiOperation("创建订单（立即购买）")
    @PostMapping
    public Result<OrderInfo> create(@RequestBody OrderCreateDTO dto, HttpServletRequest request) {
        // 越权防护：非管理员强制把订单归属改为当前登录用户，防止替他人下单
        if (!UserContext.isAdmin(request) && dto.getOrder() != null) {
            Long currentUserId = UserContext.currentUserId(request);
            if (currentUserId == null) {
                return Result.error("未登录或登录已过期");
            }
            dto.getOrder().setUserId(currentUserId);
        }
        return orderService.createOrder(dto);
    }

    @ApiOperation("购物车结算")
    @PostMapping("/checkout")
    public Result<OrderInfo> checkout(@RequestBody CheckoutDTO dto, HttpServletRequest request) {
        // 越权防护：非管理员强制使用当前登录用户，防止结算他人的购物车
        if (!UserContext.isAdmin(request)) {
            Long currentUserId = UserContext.currentUserId(request);
            if (currentUserId == null) {
                return Result.error("未登录或登录已过期");
            }
            dto.setUserId(currentUserId);
        }
        return orderService.checkout(dto);
    }

    @ApiOperation("支付订单")
    @PutMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        String err = orderAccessError(id, request);
        if (err != null) {
            return Result.error(err);
        }
        return orderService.payOrder(id);
    }

    @ApiOperation("取消订单（恢复库存）")
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        String err = orderAccessError(id, request);
        if (err != null) {
            return Result.error(err);
        }
        return orderService.cancelOrder(id);
    }

    @ApiOperation("发货（管理员/商家）")
    @PutMapping("/ship/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> ship(@PathVariable Long id) {
        return orderService.shipOrder(id);
    }

    @ApiOperation("确认收货")
    @PutMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id, HttpServletRequest request) {
        String err = orderAccessError(id, request);
        if (err != null) {
            return Result.error(err);
        }
        return orderService.confirmOrder(id);
    }

    @ApiOperation("更新订单信息（管理员）")
    @PutMapping
    @RequireRole({"ADMIN"})
    public Result<Void> update(@RequestBody OrderInfo order) {
        orderService.updateById(order);
        return Result.success();
    }

    /**
     * 校验当前用户是否有权访问指定订单。
     * 规则：管理员放行；其他用户必须是该订单的所属人。
     * @param orderId 订单 ID
     * @param request 当前请求（内含 JWT 解析出的身份）
     * @return 无权时返回错误提示；有权返回 null
     */
    private String orderAccessError(Long orderId, HttpServletRequest request) {
        if (UserContext.isAdmin(request)) {
            return null;
        }
        OrderInfo order = orderService.getById(orderId);
        if (order == null) {
            return "订单不存在";
        }
        if (!UserContext.isOwner(request, order.getUserId())) {
            log.warn("越权访问订单被拦截: orderId={}, currentUserId={}, ownerId={}",
                    orderId, UserContext.currentUserId(request), order.getUserId());
            return "无权操作该订单";
        }
        return null;
    }
}
