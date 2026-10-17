package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_seasonal")
public class ProductSeasonal {
    private Long id;
    private Long productId;
    private Integer month;
    private Integer isPeak;
    private String description;
}
