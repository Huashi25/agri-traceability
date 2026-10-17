package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("faq")
public class Faq {
    private Long id;
    private String category;
    private String question;
    private String answer;
    private Integer viewCount;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
