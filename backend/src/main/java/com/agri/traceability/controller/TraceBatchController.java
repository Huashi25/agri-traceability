package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.Product;
import com.agri.traceability.entity.TraceBatch;
import com.agri.traceability.service.ProductService;
import com.agri.traceability.service.SysUserService;
import com.agri.traceability.service.TraceBatchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 溯源批次控制器。
 *
 * 权限模型（2026-10-19 第三轮）：
 *   查询 —— /api/trace/batch/list 同时服务消费者端（扫码溯源）和后台批次管理页，
 *           本来就是公开接口。后台调用时传 mine=true，按身份收窄：
 *             MERCHANT 只看"批次所属商品归自己"的批次；
 *             FARMER   只看 farmer_id 等于自己绑定农户的批次；
 *             ADMIN    不限制，看全量。
 *           消费者端不传 mine，行为与之前完全一致。
 *   写入 —— 商家只能改自己商品的批次；农户只能改自己名下的批次，
 *           且不允许把批次改派给别人（防止"把不是我种的批次认领走"）。
 *
 * 为什么农户归属落在批次而不是商品：
 *   同一商品可能由多个农户供货，商品级的单一农户字段表达不了；
 *   批次才是一次具体的生产发货，归到批次上语义才对。
 */
@Slf4j
@RestController
@RequestMapping("/api/trace/batch")
public class TraceBatchController {

    @Autowired
    private TraceBatchService traceBatchService;
    @Autowired
    private ProductService productService;
    @Autowired
    private SysUserService userService;

    /**
     * 批次列表。
     *
     * @param productId 按商品过滤（消费者端商品详情页用）
     * @param mine      true 表示"只看我自己的"，具体范围按当前角色决定
     * @param request   用于读取当前登录身份
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long productId,
                       @RequestParam(required = false) Boolean mine,
                       HttpServletRequest request) {
        QueryWrapper<TraceBatch> wrapper = new QueryWrapper<>();
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }

        // 只有后台显式传 mine=true 才收窄；消费者端不传，行为不变
        if (Boolean.TRUE.equals(mine)) {
            String role = UserContext.currentRole(request);

            if (UserContext.ROLE_MERCHANT.equals(role)) {
                // 商家：批次所属商品必须归自己
                List<Long> myProductIds = merchantProductIds(UserContext.currentUserId(request));
                if (myProductIds.isEmpty()) {
                    return Result.success(Collections.emptyList());
                }
                wrapper.in("product_id", myProductIds);

            } else if (UserContext.ROLE_FARMER.equals(role)) {
                // 农户：只看自己绑定农户名下的批次
                Long myFarmerId = currentFarmerId(request);
                if (myFarmerId == null) {
                    // 账号没绑定农户信息 → 一条都不该看到，避免拿全量数据
                    log.warn("农户账号未绑定 farmer_id，返回空批次列表: userId={}",
                            UserContext.currentUserId(request));
                    return Result.success(Collections.emptyList());
                }
                wrapper.eq("farmer_id", myFarmerId);
            }
            // ADMIN 不做任何限制
        }

        wrapper.orderByDesc("create_time");
        return Result.success(traceBatchService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<TraceBatch> getById(@PathVariable Long id) {
        return Result.success(traceBatchService.getById(id));
    }

    /**
     * 新建批次。
     *
     * 商户端原本建不了批次（注解只放了 ADMIN/FARMER），但后台菜单对商家开放了
     * 溯源管理页，属于"看得见、点不动"的历史缺陷，这里一并放开并加归属约束。
     */
    @PostMapping
    @RequireRole({"ADMIN", "MERCHANT", "FARMER"})
    public Result<TraceBatch> create(@RequestBody TraceBatch batch, HttpServletRequest request) {
        if (batch.getProductId() == null) {
            return Result.error("请选择关联商品");
        }

        if (UserContext.isFarmer(request)) {
            // 农户：强制把批次记在自己名下，忽略请求体里的 farmerId，防伪造
            Long myFarmerId = currentFarmerId(request);
            if (myFarmerId == null) {
                return Result.error("当前账号未绑定农户信息，无法创建批次");
            }
            batch.setFarmerId(myFarmerId);

        } else if (!UserContext.isAdmin(request)) {
            // 商家：只能给自己名下的商品建批次
            Long merchantId = UserContext.currentUserId(request);
            Product product = productService.getById(batch.getProductId());
            if (product == null) {
                return Result.error("商品不存在");
            }
            if (merchantId == null || !merchantId.equals(product.getMerchantId())) {
                log.warn("越权为他人商品创建批次被拦截: productId={}, currentUserId={}",
                        batch.getProductId(), merchantId);
                return Result.error("无权为该商品创建批次");
            }
        }

        if (batch.getBatchNo() == null || batch.getBatchNo().isEmpty()) {
            batch.setBatchNo(traceBatchService.generateBatchNo());
        }
        if (batch.getTraceCode() == null || batch.getTraceCode().isEmpty()) {
            batch.setTraceCode(traceBatchService.generateTraceCode(batch.getProductId()));
        }
        if (batch.getStatus() == null) {
            batch.setStatus(0);
        }
        traceBatchService.save(batch);
        return Result.success(batch);
    }

    /**
     * 修改批次。
     *
     * 商家：批次所属商品必须归自己；
     * 农户：批次 farmer_id 必须等于自己绑定的农户，且不允许改派给别的农户；
     * 管理员：不限。
     */
    @PutMapping
    @RequireRole({"ADMIN", "MERCHANT", "FARMER"})
    public Result<Void> update(@RequestBody TraceBatch batch, HttpServletRequest request) {
        if (!UserContext.isAdmin(request)) {
            TraceBatch exists = batch.getId() == null ? null : traceBatchService.getById(batch.getId());
            if (exists == null) {
                return Result.error("批次不存在");
            }

            if (UserContext.isFarmer(request)) {
                Long myFarmerId = currentFarmerId(request);
                if (myFarmerId == null || !myFarmerId.equals(exists.getFarmerId())) {
                    log.warn("越权修改溯源批次被拦截(农户): batchId={}, currentUserId={}, farmerId={}",
                            batch.getId(), UserContext.currentUserId(request), exists.getFarmerId());
                    return Result.error("无权操作该批次");
                }
                // 农户不能把批次改派给别人：归属字段以库里为准，强制覆盖
                batch.setFarmerId(myFarmerId);

            } else {
                // 商家：按"批次所属商品是否归自己"判断
                Long merchantId = UserContext.currentUserId(request);
                Product product = exists.getProductId() == null
                        ? null : productService.getById(exists.getProductId());
                if (product == null || merchantId == null || !merchantId.equals(product.getMerchantId())) {
                    log.warn("越权修改溯源批次被拦截(商家): batchId={}, currentUserId={}, productId={}",
                            batch.getId(), merchantId, exists.getProductId());
                    return Result.error("无权操作该批次");
                }
            }
        }
        traceBatchService.updateById(batch);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        traceBatchService.removeById(id);
        return Result.success();
    }

    // ==================== 归属判定（私有工具方法） ====================

    /**
     * 取当前登录账号绑定的农户ID。
     * JWT 里没有 farmer_id，只能回查 sys_user。
     */
    private Long currentFarmerId(HttpServletRequest request) {
        return userService.getFarmerId(UserContext.currentUserId(request));
    }

    /**
     * 查询某商家名下的商品 ID 列表。
     */
    private List<Long> merchantProductIds(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.select("id").eq("merchant_id", merchantId);
        return productService.list(wrapper).stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
