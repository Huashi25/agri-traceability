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
