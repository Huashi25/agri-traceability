package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    private Long id;
    private String name;
    private Long categoryId;
    /**
     * 归属商家 ID（关联 sys_user.id，且该账号 role = MERCHANT）。
     * 为 null 表示"平台自营"，只有管理员可管理；
     * 商家角色只能操作 merchantId 等于自己的商品及其订单。
     */
    private Long merchantId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String description;
    private String image;
    private String origin;
    private Integer status;
    private Integer salesCount;
    private BigDecimal rating;
    private Integer reviewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
