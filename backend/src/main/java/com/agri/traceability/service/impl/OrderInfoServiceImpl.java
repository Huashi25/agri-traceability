package com.agri.traceability.service.impl;

import com.agri.traceability.common.CheckoutDTO;
import com.agri.traceability.common.OrderCreateDTO;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.*;
import com.agri.traceability.mapper.OrderItemMapper;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 负责订单全生命周期的业务逻辑处理
 * 采用三层架构：Controller层接收请求 → Service层处理业务 → Mapper层操作数据库
 */
@Slf4j
@Service
public class OrderInfoServiceImpl extends ServiceImpl<com.agri.traceability.mapper.OrderInfoMapper, OrderInfo>
        implements OrderInfoService {

    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CartService cartService;
    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderInfo> createOrder(OrderCreateDTO dto) {
        OrderInfo order = dto.getOrder();
        List<OrderItem> items = dto.getItems();

        if (items == null || items.isEmpty()) {
            return Result.error("商品列表不能为空");
        }

        log.info("创建订单: userId={}, itemCount={}", order.getUserId(), items.size());

        // 校验商品 + 原子扣减库存 + 填充商品信息 + 计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> finalItems = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                log.warn("创建订单失败: 商品不存在, productId={}", item.getProductId());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("商品不存在: ID=" + item.getProductId());
            }
            if (product.getStatus() != 1) {
                log.warn("创建订单失败: 商品已下架, productId={}", item.getProductId());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("商品已下架: " + product.getName());
            }
            // 原子扣减库存（数据库行锁保证并发安全，防止超卖）
            int affected = productMapper.deductStock(item.getProductId(), item.getQuantity());
            if (affected == 0) {
                log.warn("创建订单失败: 库存不足, productId={}, quantity={}", item.getProductId(), item.getQuantity());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("库存不足: " + product.getName());
            }
            // 从数据库取真实价格，不信任前端传过来的价格（防篡改）
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getSubtotal());
            finalItems.add(item);
        }

        // 生成订单号并保存订单
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setStatus(0); // 待付款
        order.setTotalAmount(totalAmount);
        order.setCreateTime(LocalDateTime.now());
        baseMapper.insert(order);

        // 保存订单明细
        for (OrderItem item : finalItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        order.setOrderItems(finalItems);
        log.info("订单创建成功: orderNo={}, totalAmount={}", order.getOrderNo(), totalAmount);
        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderInfo> checkout(CheckoutDTO dto) {
        Long userId = dto.getUserId();
        List<Long> cartIds = dto.getCartIds();

        if (cartIds == null || cartIds.isEmpty()) {
            return Result.error("请选择要结算的商品");
        }

        log.info("购物车结算: userId={}, cartIds={}", userId, cartIds.size());

        // 从购物车获取选中的商品
        QueryWrapper<Cart> cartWrapper = new QueryWrapper<>();
        cartWrapper.eq("user_id", userId).in("id", cartIds);
        List<Cart> cartItems = cartService.list(cartWrapper);

        if (cartItems.isEmpty()) {
            return Result.error("购物车中没有选中的商品");
        }

        // 构建订单明细 + 原子扣减库存
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                log.warn("结算失败: 商品不存在, productId={}", cart.getProductId());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("商品不存在: ID=" + cart.getProductId());
            }
            if (product.getStatus() != 1) {
                log.warn("结算失败: 商品已下架, productId={}", cart.getProductId());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("商品已下架: " + product.getName());
            }
            // 原子扣减库存（数据库行锁保证并发安全，防止超卖）
            int affected = productMapper.deductStock(cart.getProductId(), cart.getQuantity());
            if (affected == 0) {
                log.warn("结算失败: 库存不足, productId={}, quantity={}", cart.getProductId(), cart.getQuantity());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("库存不足: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            totalAmount = totalAmount.add(item.getSubtotal());
            orderItems.add(item);
        }

        // 创建订单
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(userId);
        order.setStatus(0); // 待付款
        order.setTotalAmount(totalAmount);
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setCreateTime(LocalDateTime.now());
        baseMapper.insert(order);

        // 保存订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 清除已结算的购物车项
        cartService.remove(cartWrapper);

        order.setOrderItems(orderItems);
        log.info("购物车结算成功: orderNo={}, totalAmount={}", order.getOrderNo(), totalAmount);
        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payOrder(Long orderId) {
        OrderInfo order = baseMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 0) return Result.error("订单状态不允许支付");
        order.setStatus(1); // 已付款
        order.setPayTime(LocalDateTime.now());
        baseMapper.updateById(order);
        log.info("订单支付成功: orderId={}, orderNo={}", orderId, order.getOrderNo());
        pushNotification(order.getUserId(), "支付成功",
                "您的订单 " + order.getOrderNo() + " 已支付成功，等待商家发货", 1, "/orders");
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrder(Long orderId) {
        OrderInfo order = baseMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            return Result.error("只能取消待付款或已付款的订单");
        }
        order.setStatus(4); // 已取消
        order.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(order);
        log.info("订单取消: orderId={}, orderNo={}, 原状态={}", orderId, order.getOrderNo(), order.getStatus());
        pushNotification(order.getUserId(), "订单已取消",
                "您的订单 " + order.getOrderNo() + " 已取消", 1, "/orders");

        // 原子恢复库存
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
        log.info("库存已恢复: orderId={}, itemCount={}", orderId, items.size());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> shipOrder(Long orderId) {
        OrderInfo order = baseMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 1) return Result.error("只能发货已付款的订单");
        order.setStatus(2); // 已发货
        order.setShipTime(LocalDateTime.now());
        baseMapper.updateById(order);
        log.info("订单发货: orderId={}, orderNo={}", orderId, order.getOrderNo());
        pushNotification(order.getUserId(), "订单已发货",
                "您的订单 " + order.getOrderNo() + " 已发货，请注意查收", 1, "/orders");
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmOrder(Long orderId) {
        OrderInfo order = baseMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 2) return Result.error("只能确认已发货的订单");
        order.setStatus(3); // 已完成
        order.setReceiveTime(LocalDateTime.now());
        baseMapper.updateById(order);

        // 更新商品销量
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSalesCount((product.getSalesCount() != null ? product.getSalesCount() : 0) + item.getQuantity());
                productMapper.updateById(product);
            }
        }
        log.info("订单确认收货: orderId={}, orderNo={}", orderId, order.getOrderNo());
        pushNotification(order.getUserId(), "订单已完成",
                "您的订单 " + order.getOrderNo() + " 已确认收货，欢迎评价分享", 1, "/orders");
        return Result.success();
    }

    @Override
    public Result listUserOrders(Long userId) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<OrderInfo> orders = baseMapper.selectList(wrapper);

        // 批量查询所有订单明细（解决N+1查询问题）
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(OrderInfo::getId).collect(Collectors.toList());
            QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.in("order_id", orderIds);
            List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);
            // 按订单ID分组
            Map<Long, List<OrderItem>> itemsMap = allItems.stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId));
            for (OrderInfo order : orders) {
                order.setOrderItems(itemsMap.get(order.getId()));
            }
        }
        return Result.success(orders);
    }

    @Override
    public Result<OrderInfo> getOrderDetail(Long orderId) {
        OrderInfo order = baseMapper.selectById(orderId);
        if (order != null) {
            QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.eq("order_id", orderId);
            order.setOrderItems(orderItemMapper.selectList(itemWrapper));
        }
        return Result.success(order);
    }

    /**
     * 推送系统通知
     */
    private void pushNotification(Long userId, String title, String content, int type, String linkUrl) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setIsRead(0);
        n.setLinkUrl(linkUrl);
        n.setCreateTime(LocalDateTime.now());
        n.setUpdateTime(LocalDateTime.now());
        notificationService.save(n);
    }
}
