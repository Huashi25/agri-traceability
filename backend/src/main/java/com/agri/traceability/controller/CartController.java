package com.agri.traceability.controller;

import com.agri.traceability.common.CartVO;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.Cart;
import com.agri.traceability.entity.Product;
import com.agri.traceability.mapper.ProductMapper;
import com.agri.traceability.service.CartService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车控制器
 *
 * 安全约定（2026-10-19 加固）：购物车是私有数据，"是谁的车"一律以 JWT 中的用户身份为准，
 * 绝不采信前端传入的 userId；修改/删除前先校验该记录是否属于当前用户。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        // 越权防护：普通用户只能看自己的购物车，前端传的 userId 直接忽略
        Long targetUserId = UserContext.isAdmin(request)
                ? (userId != null ? userId : UserContext.currentUserId(request))
                : UserContext.currentUserId(request);
        if (targetUserId == null) {
            return Result.error("未登录或登录已过期");
        }

        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", targetUserId).orderByDesc("create_time");
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
    public Result<Void> add(@RequestBody Cart cart, HttpServletRequest request) {
        // 越权防护：非管理员只能往自己购物车里加东西
        if (!UserContext.isAdmin(request)) {
            Long currentUserId = UserContext.currentUserId(request);
            if (currentUserId == null) {
                return Result.error("未登录或登录已过期");
            }
            cart.setUserId(currentUserId);
        }

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
    public Result<Void> update(@RequestBody Cart cart, HttpServletRequest request) {
        String err = cartAccessError(cart.getId(), request);
        if (err != null) {
            return Result.error(err);
        }
        cartService.updateById(cart);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        String err = cartAccessError(id, request);
        if (err != null) {
            return Result.error(err);
        }
        cartService.removeById(id);
        return Result.success();
    }

    /**
     * 校验当前用户是否有权操作指定购物车记录。
     * 规则：管理员放行；其他用户必须是该记录的所有者。
     * @param cartId 购物车记录 ID
     * @param request 当前请求（内含 JWT 解析出的身份）
     * @return 无权时返回错误提示；有权返回 null
     */
    private String cartAccessError(Long cartId, HttpServletRequest request) {
        if (cartId == null) {
            return "购物车记录不存在";
        }
        if (UserContext.isAdmin(request)) {
            return null;
        }
        Cart existing = cartService.getById(cartId);
        if (existing == null) {
            return "购物车记录不存在";
        }
        if (!UserContext.isOwner(request, existing.getUserId())) {
            return "无权操作他人的购物车";
        }
        return null;
    }
}
