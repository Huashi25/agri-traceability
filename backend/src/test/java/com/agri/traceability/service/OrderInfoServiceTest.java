package com.agri.traceability.service;

import com.agri.traceability.common.CheckoutDTO;
import com.agri.traceability.common.OrderCreateDTO;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.*;
import com.agri.traceability.mapper.OrderInfoMapper;
import com.agri.traceability.mapper.OrderItemMapper;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.impl.OrderInfoServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 * 测试OrderInfoServiceImpl的订单全生命周期业务方法
 * 重点覆盖：原子扣减库存、防超卖、库存恢复、状态流转
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("订单服务单元测试")
class OrderInfoServiceTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CartService cartService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OrderItemService orderItemService;

    private OrderInfoServiceImpl orderInfoService;
    private MockedStatic<TransactionAspectSupport> transactionMock;

    private Product testProduct;
    private OrderInfo testOrder;

    @BeforeEach
    void setUp() {
        orderInfoService = new OrderInfoServiceImpl();
        ReflectionTestUtils.setField(orderInfoService, "baseMapper", orderInfoMapper);
        ReflectionTestUtils.setField(orderInfoService, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(orderInfoService, "orderItemMapper", orderItemMapper);
        ReflectionTestUtils.setField(orderInfoService, "productMapper", productMapper);
        ReflectionTestUtils.setField(orderInfoService, "cartService", cartService);
        ReflectionTestUtils.setField(orderInfoService, "notificationService", notificationService);

        // Mock TransactionAspectSupport以支持@Transactional的单元测试
        transactionMock = mockStatic(TransactionAspectSupport.class);
        DefaultTransactionStatus mockStatus = mock(DefaultTransactionStatus.class);
        transactionMock.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("有机苹果");
        testProduct.setPrice(new BigDecimal("19.90"));
        testProduct.setStock(100);
        testProduct.setStatus(1);
        testProduct.setSalesCount(0);

        testOrder = new OrderInfo();
        testOrder.setId(100L);
        testOrder.setOrderNo("ORD1234567890");
        testOrder.setUserId(1L);
        testOrder.setStatus(0);
        testOrder.setTotalAmount(new BigDecimal("39.80"));
        testOrder.setCreateTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        if (transactionMock != null) {
            transactionMock.close();
        }
    }

    @Test
    @DisplayName("创建订单成功 - 正常购买")
    void testCreateOrder_Success() {
        OrderCreateDTO dto = new OrderCreateDTO();
        OrderInfo order = new OrderInfo();
        order.setUserId(1L);
        dto.setOrder(order);
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        dto.setItems(Arrays.asList(item));

        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 2)).thenReturn(1);
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            OrderInfo o = invocation.getArgument(0);
            o.setId(100L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<OrderInfo> result = orderInfoService.createOrder(dto);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(100L, result.getData().getId());
        assertEquals(0, result.getData().getStatus());
        assertEquals(new BigDecimal("39.80"), result.getData().getTotalAmount());
        verify(productMapper, times(1)).deductStock(1L, 2);
    }

    @Test
    @DisplayName("创建订单失败 - 商品列表为空")
    void testCreateOrder_EmptyItems() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setOrder(new OrderInfo());
        dto.setItems(new ArrayList<>());

        Result<OrderInfo> result = orderInfoService.createOrder(dto);

        assertNotEquals(200, result.getCode());
        assertEquals("商品列表不能为空", result.getMessage());
    }

    @Test
    @DisplayName("创建订单失败 - 商品不存在")
    void testCreateOrder_ProductNotFound() {
        OrderCreateDTO dto = new OrderCreateDTO();
        OrderInfo order = new OrderInfo();
        order.setUserId(1L);
        dto.setOrder(order);
        OrderItem item = new OrderItem();
        item.setProductId(999L);
        item.setQuantity(1);
        dto.setItems(Arrays.asList(item));

        when(productMapper.selectById(999L)).thenReturn(null);

        Result<OrderInfo> result = orderInfoService.createOrder(dto);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("商品不存在"));
    }

    @Test
    @DisplayName("创建订单失败 - 商品已下架")
    void testCreateOrder_ProductOffShelf() {
        testProduct.setStatus(0);
        OrderCreateDTO dto = new OrderCreateDTO();
        OrderInfo order = new OrderInfo();
        order.setUserId(1L);
        dto.setOrder(order);
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        dto.setItems(Arrays.asList(item));

        when(productMapper.selectById(1L)).thenReturn(testProduct);

        Result<OrderInfo> result = orderInfoService.createOrder(dto);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("已下架"));
    }

    @Test
    @DisplayName("创建订单失败 - 库存不足（防超卖）")
    void testCreateOrder_InsufficientStock() {
        testProduct.setStock(3);
        OrderCreateDTO dto = new OrderCreateDTO();
        OrderInfo order = new OrderInfo();
        order.setUserId(1L);
        dto.setOrder(order);
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(5);
        dto.setItems(Arrays.asList(item));

        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 5)).thenReturn(0);

        Result<OrderInfo> result = orderInfoService.createOrder(dto);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("库存不足"));
        verify(productMapper, times(1)).deductStock(1L, 5);
    }

    @Test
    @DisplayName("购物车结算成功")
    void testCheckout_Success() {
        CheckoutDTO dto = new CheckoutDTO();
        dto.setUserId(1L);
        dto.setCartIds(Arrays.asList(10L, 20L));
        dto.setReceiverName("张三");
        dto.setReceiverPhone("13800000001");
        dto.setReceiverAddress("北京市朝阳区");

        Cart cart1 = new Cart();
        cart1.setId(10L);
        cart1.setUserId(1L);
        cart1.setProductId(1L);
        cart1.setQuantity(2);

        when(cartService.list(any(QueryWrapper.class))).thenReturn(Arrays.asList(cart1));
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 2)).thenReturn(1);
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            OrderInfo o = invocation.getArgument(0);
            o.setId(200L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(cartService.remove(any(QueryWrapper.class))).thenReturn(true);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<OrderInfo> result = orderInfoService.checkout(dto);

        assertEquals(200, result.getCode());
        assertEquals(200L, result.getData().getId());
        assertEquals("张三", result.getData().getReceiverName());
        verify(productMapper, times(1)).deductStock(1L, 2);
        verify(cartService, times(1)).remove(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("购物车结算失败 - 未选择商品")
    void testCheckout_NoCartIds() {
        CheckoutDTO dto = new CheckoutDTO();
        dto.setUserId(1L);
        dto.setCartIds(new ArrayList<>());

        Result<OrderInfo> result = orderInfoService.checkout(dto);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("请选择"));
    }

    @Test
    @DisplayName("支付订单成功")
    void testPayOrder_Success() {
        testOrder.setStatus(0);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<Void> result = orderInfoService.payOrder(100L);

        assertEquals(200, result.getCode());
        assertEquals(1, testOrder.getStatus());
        assertNotNull(testOrder.getPayTime());
    }

    @Test
    @DisplayName("支付订单失败 - 订单不存在")
    void testPayOrder_OrderNotFound() {
        when(orderInfoMapper.selectById(999L)).thenReturn(null);

        Result<Void> result = orderInfoService.payOrder(999L);

        assertNotEquals(200, result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }

    @Test
    @DisplayName("支付订单失败 - 状态不允许（重复支付）")
    void testPayOrder_WrongStatus() {
        testOrder.setStatus(1);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);

        Result<Void> result = orderInfoService.payOrder(100L);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("不允许"));
    }

    @Test
    @DisplayName("取消订单成功 - 待付款订单")
    void testCancelOrder_PendingOrder() {
        testOrder.setStatus(0);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item));
        when(productMapper.restoreStock(1L, 2)).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<Void> result = orderInfoService.cancelOrder(100L);

        assertEquals(200, result.getCode());
        assertEquals(4, testOrder.getStatus());
        verify(productMapper, times(1)).restoreStock(1L, 2);
    }

    @Test
    @DisplayName("取消订单成功 - 已付款订单")
    void testCancelOrder_PaidOrder() {
        testOrder.setStatus(1);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item));
        when(productMapper.restoreStock(1L, 2)).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<Void> result = orderInfoService.cancelOrder(100L);

        assertEquals(200, result.getCode());
        assertEquals(4, testOrder.getStatus());
        verify(productMapper, times(1)).restoreStock(1L, 2);
    }

    @Test
    @DisplayName("取消订单失败 - 已发货订单不能取消")
    void testCancelOrder_ShippedOrder() {
        testOrder.setStatus(2);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);

        Result<Void> result = orderInfoService.cancelOrder(100L);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("只能取消"));
        verify(productMapper, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("发货成功")
    void testShipOrder_Success() {
        testOrder.setStatus(1);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<Void> result = orderInfoService.shipOrder(100L);

        assertEquals(200, result.getCode());
        assertEquals(2, testOrder.getStatus());
        assertNotNull(testOrder.getShipTime());
    }

    @Test
    @DisplayName("发货失败 - 待付款订单不能发货")
    void testShipOrder_PendingOrder() {
        testOrder.setStatus(0);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);

        Result<Void> result = orderInfoService.shipOrder(100L);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("只能发货已付款"));
    }

    @Test
    @DisplayName("确认收货成功 - 更新销量")
    void testConfirmOrder_Success() {
        testOrder.setStatus(2);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(3);
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item));
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        Result<Void> result = orderInfoService.confirmOrder(100L);

        assertEquals(200, result.getCode());
        assertEquals(3, testOrder.getStatus());
        assertNotNull(testOrder.getReceiveTime());
        assertEquals(3, testProduct.getSalesCount());
    }

    @Test
    @DisplayName("确认收货失败 - 未发货订单不能确认")
    void testConfirmOrder_NotShipped() {
        testOrder.setStatus(1);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);

        Result<Void> result = orderInfoService.confirmOrder(100L);

        assertNotEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("只能确认已发货"));
    }

    @Test
    @DisplayName("查询用户订单列表 - 批量加载明细（N+1优化验证）")
    void testListUserOrders_BatchLoadItems() {
        OrderInfo order1 = new OrderInfo();
        order1.setId(1L);
        order1.setOrderNo("ORD001");
        order1.setUserId(1L);
        OrderInfo order2 = new OrderInfo();
        order2.setId(2L);
        order2.setOrderNo("ORD002");
        order2.setUserId(1L);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrderId(1L);
        item1.setProductName("苹果");
        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrderId(2L);
        item2.setProductName("橙子");

        when(orderInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(order1, order2));
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item1, item2));

        Result result = orderInfoService.listUserOrders(1L);

        assertEquals(200, result.getCode());
        List<OrderInfo> resultList = (List<OrderInfo>) result.getData();
        assertEquals(2, resultList.size());
        verify(orderItemMapper, times(1)).selectList(any(QueryWrapper.class));
        assertEquals(1, resultList.get(0).getOrderItems().size());
    }

    @Test
    @DisplayName("查询订单详情 - 包含明细")
    void testGetOrderDetail_WithItems() {
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setProductName("有机苹果");
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item));

        Result<OrderInfo> result = orderInfoService.getOrderDetail(100L);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().getOrderItems());
        assertEquals(1, result.getData().getOrderItems().size());
    }

    @Test
    @DisplayName("订单完整状态流转: 待付款→已付款→已发货→已完成")
    void testOrderFullLifecycle() {
        testOrder.setStatus(0);
        when(orderInfoMapper.selectById(100L)).thenReturn(testOrder);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);
        when(notificationService.save(any(Notification.class))).thenReturn(true);

        // 支付
        Result<Void> payResult = orderInfoService.payOrder(100L);
        assertEquals(200, payResult.getCode());
        assertEquals(1, testOrder.getStatus());

        // 发货
        Result<Void> shipResult = orderInfoService.shipOrder(100L);
        assertEquals(200, shipResult.getCode());
        assertEquals(2, testOrder.getStatus());

        // 确认收货
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        when(orderItemMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(item));
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        Result<Void> confirmResult = orderInfoService.confirmOrder(100L);
        assertEquals(200, confirmResult.getCode());
        assertEquals(3, testOrder.getStatus());
    }
}
