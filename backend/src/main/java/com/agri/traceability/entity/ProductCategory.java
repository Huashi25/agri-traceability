package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_category")
public class ProductCategory {
    private Long id;
    private String name;
    private String description;
    private Integer sort;
    private LocalDateTime createTime;
}
