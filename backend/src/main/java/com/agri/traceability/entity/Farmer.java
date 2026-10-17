package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("farmer_info")
public class Farmer {
    private Long id;
    private String name;
    private String phone;
    private String farmName;
    private String location;
    private BigDecimal farmArea;
    private String mainCrop;
    private String description;
    private Integer status;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String regionTags;
    private String regionDescription;
    private String regionImage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
