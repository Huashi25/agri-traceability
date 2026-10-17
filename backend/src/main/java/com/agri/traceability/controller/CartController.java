package com.agri.traceability.controller;

import com.agri.traceability.common.CartVO;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Cart;
import com.agri.traceability.entity.Product;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.CartService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/list")
    public Result list(@RequestParam Long userId) {
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<Cart> carts = cartService.list(wrapper);
        List<CartVO> voList = new ArrayList<>();

        // 批量查询商品（解决N+1查询问题）
        if (!carts.isEmpty()) {
            List<Long> productIds = carts.stream()
                    .map(Cart::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Product> products = productMapper.selectBatchIds(productIds);
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (Cart cart : carts) {
                CartVO vo = new CartVO();
                vo.setCart(cart);
                vo.setProduct(productMap.get(cart.getProductId()));
                voList.add(vo);
            }
        }
        return Result.success(voList);
    }

    @PostMapping
    public Result<Void> add(@RequestBody Cart cart) {
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", cart.getUserId()).eq("product_id", cart.getProductId());
        Cart existing = cartService.getOne(wrapper);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + (cart.getQuantity() == null ? 1 : cart.getQuantity()));
            cartService.updateById(existing);
        } else {
            if (cart.getQuantity() == null) {
                cart.setQuantity(1);
            }
            cartService.save(cart);
        }
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Cart cart) {
        cartService.updateById(cart);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.removeById(id);
        return Result.success();
    }
}
