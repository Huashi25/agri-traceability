package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质量标准信息实体
 */
@Data
@TableName("quality_standards")
public class QualityStandard {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sortNum;
    private String title;
    private String code;
    private String description;
    private String icon;
    private LocalDateTime createTime;
}
