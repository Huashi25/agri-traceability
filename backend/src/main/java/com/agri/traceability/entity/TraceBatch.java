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
    /**
     * 供货农户ID（关联 farmer_info.id）。
     * NULL 表示未指定农户，属于平台/商家代管，只有管理员能管。
     * 农户账号登录后只应看到 farmer_id 等于自己绑定农户的批次。
     */
    private Long farmerId;
    private String traceCode;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
