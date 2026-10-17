package com.agri.traceability.service.impl;

import com.agri.traceability.entity.Cart;
import com.agri.traceability.mapper.CartMapper;
import com.agri.traceability.service.CartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {
}
