package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("origin_info")
public class OriginInfo {
    private Long id;
    private Long batchId;
    private String farmName;
    private String location;
    private String farmerName;
    private LocalDate plantDate;
    private LocalDate harvestDate;
    private String description;
    private LocalDateTime createTime;
}
