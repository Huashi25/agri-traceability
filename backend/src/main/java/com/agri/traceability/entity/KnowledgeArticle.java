package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String category;
    private String tags;
    private Integer viewCount;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
