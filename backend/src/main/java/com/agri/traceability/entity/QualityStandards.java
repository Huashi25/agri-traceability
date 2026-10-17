package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("quality_standards")
public class QualityStandards {
    private Long id;
    private Long categoryId;
    private String standardName;
    private String standardCode;
    private String description;
    private String requirements;
    private String testMethod;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
