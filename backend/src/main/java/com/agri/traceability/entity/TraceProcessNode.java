package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 溯源流程节点实体
 */
@Data
@TableName("trace_process_nodes")
public class TraceProcessNode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sortNum;
    private String title;
    private String stage;
    private String description;
    private String icon;
    private String color;
    private LocalDateTime createTime;
}
