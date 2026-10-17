package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistics_info")
public class LogisticsInfo {
    private Long id;
    private Long batchId;
    private LocalDateTime shipDate;
    private String fromLocation;
    private String toLocation;
    private String transportCompany;
    private String transportMethod;
    private String temperature;
    private LocalDateTime arrivalDate;
    private LocalDateTime createTime;
}
