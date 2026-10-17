package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recipe")
public class Recipe {
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private Integer cookTime;
    private Integer servings;
    private String difficulty;
    private String steps;
    private String category;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
