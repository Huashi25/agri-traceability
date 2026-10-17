package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("quality_inspection")
public class QualityInspection {
    private Long id;
    private Long batchId;
    private String inspector;
    private LocalDateTime inspectionDate;
    private String inspectionItem;
    private String result;
    private Integer qualified;
    private LocalDateTime createTime;
}
