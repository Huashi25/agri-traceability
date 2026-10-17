package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trace_batch")
public class TraceBatch {
    private Long id;
    private String batchNo;
    private Long productId;
    private String traceCode;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
