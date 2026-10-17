package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Integer type;
    private Integer isRead;
    private String linkUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
