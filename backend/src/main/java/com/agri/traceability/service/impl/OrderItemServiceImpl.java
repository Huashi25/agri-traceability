package com.agri.traceability.service.impl;

import com.agri.traceability.entity.OrderItem;
import com.agri.traceability.mapper.OrderItemMapper;
import com.agri.traceability.service.OrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
