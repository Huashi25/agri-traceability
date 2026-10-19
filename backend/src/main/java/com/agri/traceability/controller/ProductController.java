package com.agri.traceability.controller;

import com.agri.traceability.common.PageResult;
import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.Product;
import com.agri.traceability.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

/**
 * 商品控制器。
 *
 * 权限模型（2026-10-19 补）：
 *   - 查询：/api/product/list 同时服务消费者端和后台商品管理页，
 *           公开可访问。后台传 mine=true 时，商家只看到自己名下商品；
 *           消费者端不传该参数，行为与改造前一致。
 *   - 写操作：ADMIN 可管全部；MERCHANT 只能管 merchant_id 等于自己的商品，
 *           且新增/修改时归属字段被强制覆盖，无法把商品挂到别人名下。
 */
@Slf4j
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 商品列表。
     *
     * @param name      商品名模糊查询
     * @param categoryId 分类过滤
     * @param page      页码（与 pageSize 同时传才分页）
     * @param pageSize  每页条数
     * @param mine      仅商家后台使用：true 表示只查当前商家名下商品
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String name,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer pageSize,
                       @RequestParam(required = false) Boolean mine,
                       HttpServletRequest request) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        // 商家后台「只看我的商品」。管理员传 mine 也仍然看全部，便于总览。
        if (Boolean.TRUE.equals(mine) && UserContext.ROLE_MERCHANT.equals(UserContext.currentRole(request))) {
            Long merchantId = UserContext.currentUserId(request);
            // 取不到身份时用 -1 兜底，保证查不到任何数据而不是退化为全量
            wrapper.eq("merchant_id", merchantId == null ? -1L : merchantId);
        }
        wrapper.orderByDesc("create_time");
        if (page != null && pageSize != null) {
            IPage<Product> pageResult = productService.page(new Page<>(page, pageSize), wrapper);
            return Result.success(PageResult.of(pageResult));
        }
        return Result.success(productService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /**
     * 新增商品。商家新增时强制把归属设为自己。
     */
    @PostMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> save(@RequestBody Product product, HttpServletRequest request) {
        if (!UserContext.isAdmin(request)) {
            Long merchantId = UserContext.currentUserId(request);
            // saveOrUpdate 在带 id 时等价于更新，这里同样要做归属校验，
            // 否则可以借"新增"接口绕过 update 的归属限制
            if (product.getId() != null) {
                String error = productAccessError(product.getId(), merchantId);
                if (error != null) {
                    log.warn("越权修改商品被拦截(新增入口): productId={}, currentUserId={}",
                            product.getId(), merchantId);
                    return Result.error(error);
                }
            }
            product.setMerchantId(merchantId);
        }
        productService.saveOrUpdate(product);
        return Result.success();
    }

    /**
     * 修改商品。商家只能改自己的商品，且不允许把归属改到别人名下。
     */
    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> update(@RequestBody Product product, HttpServletRequest request) {
        if (!UserContext.isAdmin(request)) {
            Long merchantId = UserContext.currentUserId(request);
            String error = productAccessError(product.getId(), merchantId);
            if (error != null) {
                log.warn("越权修改商品被拦截: productId={}, currentUserId={}",
                        product.getId(), merchantId);
                return Result.error(error);
            }
            product.setMerchantId(merchantId);
        }
        // 管理员把归属清空（改回"平台自营"）时，updateById 默认忽略 null 字段，
        // 只靠它无法把 merchant_id 改回 NULL —— 必须再用 UpdateWrapper 显式 set null
        boolean clearMerchant = UserContext.isAdmin(request)
                && product.getId() != null
                && product.getMerchantId() == null;

        // 若除主键外没有任何非空字段（典型场景：只传 id + merchantId=null 改回平台自营），
        // updateById 会拼出 `UPDATE product WHERE id=?` 这种没有 SET 子句的非法 SQL，
        // 因此必须先判断再决定要不要调用
        if (hasUpdatableField(product)) {
            productService.updateById(product);
        }

        if (clearMerchant) {
            UpdateWrapper<Product> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", product.getId()).set("merchant_id", null);
            productService.update(wrapper);
        }
        return Result.success();
    }

    /**
     * 删除商品。商家只能删自己的商品。
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!UserContext.isAdmin(request)) {
            Long merchantId = UserContext.currentUserId(request);
            String error = productAccessError(id, merchantId);
            if (error != null) {
                log.warn("越权删除商品被拦截: productId={}, currentUserId={}", id, merchantId);
                return Result.error(error);
            }
        }
        productService.removeById(id);
        return Result.success();
    }

    // ==================== 归属判定（私有工具方法） ====================

    /**
     * 判断实体除主键外是否还存在非空字段。
     *
     * 用途：MyBatis-Plus 的 updateById 在没有任何可更新字段时，
     * 会生成 `UPDATE product WHERE id=?`（缺 SET 子句）导致 SQL 语法错误。
     * "只把归属改回平台自营"这种只传 id + merchantId=null 的请求正好命中这个坑，
     * 所以调用前先判断一次。
     *
     * @param product 待更新实体
     * @return 存在可更新字段返回 true
     */
    private boolean hasUpdatableField(Product product) {
        for (Field field : Product.class.getDeclaredFields()) {
            if ("id".equals(field.getName())) {
                continue; // 主键不进 SET 子句
            }
            field.setAccessible(true);
            try {
                if (field.get(product) != null) {
                    return true;
                }
            } catch (IllegalAccessException e) {
                // 反射取值失败属于异常情况，按"有值"处理，避免该更新的字段被漏掉
                return true;
            }
        }
        return false;
    }

    /**
     * 校验商家是否有权操作指定商品。
     *
     * @param productId  商品 ID
     * @param merchantId 当前商家 ID
     * @return 无权限时返回提示文案；有权限返回 null
     */
    private String productAccessError(Long productId, Long merchantId) {
        if (productId == null) {
            return "商品ID不能为空";
        }
        if (merchantId == null) {
            return "登录状态已失效，请重新登录";
        }
        Product exists = productService.getById(productId);
        if (exists == null) {
            return "商品不存在";
        }
        if (!merchantId.equals(exists.getMerchantId())) {
            return "无权操作该商品";
        }
        return null;
    }
}
