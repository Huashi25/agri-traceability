package com.agri.traceability.service.impl;

import com.agri.traceability.entity.Recipe;
import com.agri.traceability.mapper.RecipeMapper;
import com.agri.traceability.service.RecipeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements RecipeService {
}
