package com.agri.traceability.service;

import com.agri.traceability.common.CheckoutDTO;
import com.agri.traceability.common.OrderCreateDTO;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单服务接口
 * 负责订单全生命周期的业务逻辑处理
 */
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 创建订单（立即购买）
     * @param dto 订单创建参数
     * @return 订单信息
     */
    Result<OrderInfo> createOrder(OrderCreateDTO dto);

    /**
     * 购物车结算
     * @param dto 结算参数
     * @return 订单信息
     */
    Result<OrderInfo> checkout(CheckoutDTO dto);

    /**
     * 支付订单
     * @param orderId 订单ID
     * @return 操作结果
     */
    Result<Void> payOrder(Long orderId);

    /**
     * 取消订单
     * 支持取消待付款和已付款订单，取消后自动恢复库存
     * @param orderId 订单ID
     * @return 操作结果
     */
    Result<Void> cancelOrder(Long orderId);

    /**
     * 发货
     * @param orderId 订单ID
     * @return 操作结果
     */
    Result<Void> shipOrder(Long orderId);

    /**
     * 确认收货
     * @param orderId 订单ID
     * @return 操作结果
     */
    Result<Void> confirmOrder(Long orderId);

    /**
     * 查询用户订单列表（含明细）
     * @param userId 用户ID
     * @return 订单列表
     */
    Result listUserOrders(Long userId);

    /**
     * 查询订单详情（含明细）
     * @param orderId 订单ID
     * @return 订单详情
     */
    Result<OrderInfo> getOrderDetail(Long orderId);
}
