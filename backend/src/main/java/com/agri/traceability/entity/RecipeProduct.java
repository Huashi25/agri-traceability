package com.agri.traceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("recipe_product")
public class RecipeProduct {
    private Long id;
    private Long recipeId;
    private Long productId;
    private String amount;
    private Integer isMainIngredient;
}
