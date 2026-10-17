package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_review")
public class ProductReview {
    private Long id;
    private Long productId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String content;
    private String images;
    private Integer status;
    private LocalDateTime createTime;
}
